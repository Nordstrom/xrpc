#include "java_generator.h"

#include <google/protobuf/compiler/java/java_names.h>
#include <google/protobuf/io/printer.h>

// Stringify helpers used solely to cast XRPC_VERSION
#ifndef STR
#define STR(s) #s
#endif

#ifndef XSTR
#define XSTR(s) STR(s)
#endif

#ifndef FALLTHROUGH_INTENDED
#define FALLTHROUGH_INTENDED
#endif

namespace java_xrpc_generator {

using google::protobuf::FileDescriptor;
using google::protobuf::ServiceDescriptor;
using google::protobuf::MethodDescriptor;
using google::protobuf::Descriptor;
using google::protobuf::io::Printer;
using google::protobuf::SourceLocation;
using std::to_string;

// Adjust a method name prefix identifier to follow the JavaBean spec:
//   - decapitalize the first letter
//   - remove embedded underscores & capitalize the following letter
static string MixedLower(const string &word) {
  string w;
  w += tolower(word[0]);
  bool after_underscore = false;
  for (size_t i = 1; i < word.length(); ++i) {
    if (word[i] == '_') {
      after_underscore = true;
    } else {
      w += after_underscore ? toupper(word[i]) : word[i];
      after_underscore = false;
    }
  }
  return w;
}

static inline string LowerMethodName(const MethodDescriptor *method) {
  return MixedLower(method->name());
}

static inline string MessageFullJavaName(const Descriptor *desc) {
  string name = google::protobuf::compiler::java::ClassName(desc);
  return name;
}

static inline string MessageSimpleJavaName(const Descriptor *desc) {
  string name = google::protobuf::compiler::java::ClassName(desc);
  size_t last_dot = name.find_last_of(".");
  return name.substr(last_dot + 1);
}

template<typename ITR>
static void SplitStringToIteratorUsing(const string &full,
                                       const char *delim,
                                       ITR &result) {
  // Optimize the common case where delim is a single character.
  if (delim[0] != '\0' && delim[1] == '\0') {
    char c = delim[0];
    const char *p = full.data();
    const char *end = p + full.size();
    while (p != end) {
      if (*p == c) {
        ++p;
      } else {
        const char *start = p;
        while (++p != end && *p != c);
        *result++ = string(start, p - start);
      }
    }
    return;
  }

  string::size_type begin_index, end_index;
  begin_index = full.find_first_not_of(delim);
  while (begin_index != string::npos) {
    end_index = full.find_first_of(delim, begin_index);
    if (end_index == string::npos) {
      *result++ = full.substr(begin_index);
      return;
    }
    *result++ = full.substr(begin_index, (end_index - begin_index));
    begin_index = full.find_first_not_of(delim, end_index);
  }
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
static void SplitStringUsing(const string &full,
                             const char *delim,
                             std::vector<string> *result) {
  std::back_insert_iterator<std::vector<string> > it(*result);
  SplitStringToIteratorUsing(full, delim, it);
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
static std::vector<string> Split(const string &full, const char *delim) {
  std::vector<string> result;
  SplitStringUsing(full, delim, &result);
  return result;
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
static string EscapeJavadoc(const string &input) {
  string result;
  result.reserve(input.size() * 2);

  char prev = '*';

  for (char c : input) {
    switch (c) {
      case '*':
        // Avoid "/*".
        if (prev == '/') {
          result.append("&#42;");
        } else {
          result.push_back(c);
        }
        break;
      case '/':
        // Avoid "*/".
        if (prev == '*') {
          result.append("&#47;");
        } else {
          result.push_back(c);
        }
        break;
      case '@':
        // '@' starts javadoc tags including the @deprecated tag, which will
        // cause a compile-time error if inserted before a declaration that
        // does not have a corresponding @Deprecated annotation.
        result.append("&#64;");
        break;
      case '<':
        // Avoid interpretation as HTML.
        result.append("&lt;");
        break;
      case '>':
        // Avoid interpretation as HTML.
        result.append("&gt;");
        break;
      case '&':
        // Avoid interpretation as HTML.
        result.append("&amp;");
        break;
      case '\\':
        // Java interprets Unicode escape sequences anywhere!
        result.append("&#92;");
        break;
      default:result.push_back(c);
        break;
    }

    prev = c;
  }

  return result;
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
template<typename DescriptorType>
static string GetCommentsForDescriptor(const DescriptorType *descriptor) {
  SourceLocation location;
  if (descriptor->GetSourceLocation(&location)) {
    return location.leading_comments.empty() ?
           location.trailing_comments : location.leading_comments;
  }
  return string();
}

static std::vector<string> GetDocLines(const string &comments) {
  if (!comments.empty()) {
    // TODO(kenton):  Ideally we should parse the comment text as Markdown and
    //   write it back as HTML, but this requires a Markdown parser.  For now
    //   we just use <pre> to get fixed-width text formatting.

    // If the comment itself contains block comment start or end markers,
    // HTML-escape them so that they don't accidentally close the doc comment.
    string escapedComments = EscapeJavadoc(comments);

    std::vector<string> lines = Split(escapedComments, "\n");
    while (!lines.empty() && lines.back().empty()) {
      lines.pop_back();
    }
    return lines;
  }
  return std::vector<string>();
}

template<typename DescriptorType>
static std::vector<string> GetDocLinesForDescriptor(const DescriptorType *descriptor) {
  return GetDocLines(GetCommentsForDescriptor(descriptor));
}

static void WriteDocCommentBody(Printer *printer,
                                const std::vector<string> &lines,
                                bool surroundWithPreTag) {
  if (!lines.empty()) {
    if (surroundWithPreTag) {
      printer->Print(" * <pre>\n");
    }

    for (const auto &line : lines) {
      // Most lines should start with a space.  Watch out for lines that start
      // with a /, since putting that right after the leading asterisk will
      // close the comment.
      if (!line.empty() && line[0] == '/') {
        printer->Print(" * $line$\n", "line", line);
      } else {
        printer->Print(" *$line$\n", "line", line);
      }
    }

    if (surroundWithPreTag) {
      printer->Print(" * </pre>\n");
    }
  }
}

static void WriteDocComment(Printer *printer, const string &comments) {
  printer->Print("/**\n");
  std::vector<string> lines = GetDocLines(comments);
  WriteDocCommentBody(printer, lines, false);
  printer->Print(" */\n");
}

static void WriteServiceDocComment(Printer *printer,
                                   const ServiceDescriptor *service) {
  // Deviating from protobuf to avoid extraneous docs
  // (see https://github.com/google/protobuf/issues/1406);
  printer->Print("/**\n");
  std::vector<string> lines = GetDocLinesForDescriptor(service);
  WriteDocCommentBody(printer, lines, true);
  printer->Print(" */\n");
}

void WriteMethodDocComment(Printer *printer,
                           const MethodDescriptor *method) {
  // Deviating from protobuf to avoid extraneous docs
  // (see https://github.com/google/protobuf/issues/1406);
  printer->Print("/**\n");
  std::vector<string> lines = GetDocLinesForDescriptor(method);
  WriteDocCommentBody(printer, lines, true);
  printer->Print(" */\n");
}

static void PrintMethods(const ServiceDescriptor *service,
                         std::map<string, string> *vars,
                         Printer *p) {
  p->Print("// Interface methods that strictly reflect the proto.\n\n");
  (*vars)["service_name"] = service->name();

  for (int i = 0; i < service->method_count(); ++i) {
    const MethodDescriptor *method = service->method(i);
    WriteMethodDocComment(p, method);
    (*vars)["input_type"] = MessageSimpleJavaName(method->input_type());
    (*vars)["output_type"] = MessageSimpleJavaName(method->output_type());
    (*vars)["lower_method_name"] = LowerMethodName(method);
    p->Print(*vars, "$output_type$ $lower_method_name$($input_type$ input);\n\n");
  }
}

static void PrintRoutes(const ServiceDescriptor *service,
                        std::map<string, string> *vars,
                        Printer *p) {
  (*vars)["package_name"] = service->file()->package();
  (*vars)["service_name"] = service->name();

  p->Print(
      "\n"
      "/** Get defined routes for this service. */\n"
      "@Override\n"
      "default Routes routes() {\n");

  p->Indent();
  p->Print("RouteBuilder routes = new RouteBuilder();\n\n");

  for (int i = 0; i < service->method_count(); ++i) {
    const MethodDescriptor *method = service->method(i);
    (*vars)["input_type"] = MessageSimpleJavaName(method->input_type());
    (*vars)["output_type"] = MessageSimpleJavaName(method->output_type());
    (*vars)["method_name"] = method->name();
    (*vars)["lower_method_name"] = LowerMethodName(method);
    p->Print(
        *vars,
        "routes.post(\"/$service_name$/$method_name$\", request -> {\n"
        "  $input_type$ input = request.body($input_type$.class);\n"
        "  $output_type$ output = $lower_method_name$(input);\n"
        "  return request.ok(output);\n"
        "});\n\n");
  }

  p->Print("return routes;\n");
  p->Outdent();
  p->Print("}\n");
}

static void PrintService(const ServiceDescriptor *service,
                         std::map<string, string> *vars,
                         Printer *p) {
  (*vars)["service_name"] = service->name();
  (*vars)["file_name"] = service->file()->name();
  (*vars)["service_class_name"] = service->name() + "Xrpc";
  WriteServiceDocComment(p, service);
  p->Print(
      *vars,
      "@Generated(\n"
      "    value = \"by xRPC proto compiler\",\n"
      "    comments = \"Source: $file_name$\")\n"
      "public interface $service_class_name$ extends Service {\n\n");

  p->Indent();

  PrintMethods(service, vars, p);
  PrintRoutes(service, vars, p);

  p->Outdent();
  p->Print("}\n");
}

void PrintImports(const ServiceDescriptor *service,
                  std::map<string, string> *vars,
                  Printer *p) {

  set<string> imports;
  set<string>::iterator it;

  for (int i = 0; i < service->method_count(); ++i) {
    const MethodDescriptor *method = service->method(i);
    const string inputImport = MessageFullJavaName(method->input_type());
    const string outputImport = MessageFullJavaName(method->output_type());
    imports.insert(inputImport);
    imports.insert(outputImport);
  }

  for (it=imports.begin(); it!=imports.end(); ++it) {
    (*vars)["import"] = *it;
    p->Print(
        *vars,
        "import static $import$;\n");
  }

  p->Print(
      "\n"
      "import com.nordstrom.xrpc.server.RouteBuilder;\n"
      "import com.nordstrom.xrpc.server.Routes;\n"
      "import com.nordstrom.xrpc.server.Service;\n"
      "import javax.annotation.Generated;\n\n");
}

void GenerateService(const ServiceDescriptor *service,
                     google::protobuf::io::ZeroCopyOutputStream *out) {
  // All non-generated classes must be referred by fully qualified names to
  // avoid collision with generated classes.
  std::map<string, string> vars;
  vars["package_name"] = "Xrpc";
  Printer printer(out, '$');
  string package_name = ServiceJavaPackage(service->file());
  if (!package_name.empty()) {
    vars["package_name"] = package_name;
    printer.Print(
        "package $package_name$;\n\n",
        "package_name", package_name);
  }
  PrintImports(service, &vars, &printer);

  // Package string is used to fully qualify method names.
  vars["Package"] = service->file()->package();
  if (!vars["Package"].empty()) {
    vars["Package"].append(".");
  }
  PrintService(service, &vars, &printer);
}

string ServiceJavaPackage(const FileDescriptor *file) {
  string result = google::protobuf::compiler::java::ClassName(file);
  size_t last_dot_pos = result.find_last_of('.');
  if (last_dot_pos != string::npos) {
    result.resize(last_dot_pos);
  } else {
    result = "";
  }
  return result;
}

string ServiceClassName(const google::protobuf::ServiceDescriptor *service) {
  return service->name() + "Xrpc";
}

}  // namespace java_grpc_generator
