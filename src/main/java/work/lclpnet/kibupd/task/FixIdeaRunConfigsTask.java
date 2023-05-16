package work.lclpnet.kibupd.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.w3c.dom.*;
import work.lclpnet.kibupd.KibuGradlePlugin;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class FixIdeaRunConfigsTask extends DefaultTask {

    private static String joinVmOption(Map.Entry<String, String> prop) {
        return "-D%s=%s".formatted(prop.getKey(), prop.getValue());
    }

    @InputFiles
    public abstract ConfigurableFileCollection getInputFiles();

    @InputFiles
    public abstract ConfigurableFileCollection getClasspathExcludes();

    private final Map<String, String> systemProperties = new HashMap<>();

    public FixIdeaRunConfigsTask() {
        setGroup(KibuGradlePlugin.TASK_GROUP);
    }

    public void systemProperty(String key, String value) {
        systemProperties.put(key, value);
    }

    @TaskAction
    public void execute() {
        try {
            for (File file : getInputFiles().getFiles()) {
                fixFile(file.toPath());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fix run configurations", e);
        }
    }

    private void fixFile(Path file) throws Exception {
        // parse xml
        Document document;
        try (var in = Files.newInputStream(file)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(in);
        }

        // modify xml
        NodeList configurations = document.getElementsByTagName("configuration");
        if (configurations.getLength() == 0) {
            getLogger().warn("No element of type configuration was found");
            return;
        }

        for (int i = 0; i < configurations.getLength(); i++) {
            Node configuration = configurations.item(i);
            modifyConfiguration(document, configuration);
        }

        // write xml
        try (var xsltIn = getClass().getClassLoader().getResourceAsStream("xslt/format.xslt");
             var out = Files.newOutputStream(file)) {

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(xsltIn));

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(out);

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
        }
    }

    private void modifyConfiguration(Document document, Node configuration) {
        final NodeList childNodes = configuration.getChildNodes();

        Node classpathModifications = null;
        Node vmParams = null;

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);

            switch (child.getNodeName()) {
                case "classpathModifications" -> {
                    if (classpathModifications == null) {
                        classpathModifications = child;
                    }
                }
                case "option" -> {
                    NamedNodeMap attributes = child.getAttributes();
                    if (attributes == null) break;

                    Node nameAttr = attributes.getNamedItem("name");
                    if (nameAttr == null) break;

                    String name = nameAttr.getTextContent();
                    if ("VM_PARAMETERS".equals(name) && vmParams == null) {
                        vmParams = child;
                    }
                }
                default -> {}
            }
        }

        if (classpathModifications == null) {
            classpathModifications = document.createElement("classpathModifications");
            configuration.appendChild(classpathModifications);
        }

        applyClassPathModifications(document, classpathModifications);

        if (vmParams == null) {
            Element optionElement = document.createElement("option");
            optionElement.setAttribute("name", "VM_PARAMETERS");
            vmParams = optionElement;
            configuration.appendChild(vmParams);
        }

        adjustVmParam((Element) vmParams);
    }

    private void adjustVmParam(Element vmParams) {
        NamedNodeMap attributes = vmParams.getAttributes();
        Node valueAttr = attributes.getNamedItem("value");

        if (valueAttr == null) {
            String value = systemProperties.entrySet().stream()
                    .map(FixIdeaRunConfigsTask::joinVmOption)
                    .collect(Collectors.joining(" "));

            vmParams.setAttribute("value", value);
        } else {
            final String currentValue = valueAttr.getTextContent().trim();

            String append = systemProperties.entrySet().stream()
                    .filter(entry -> !currentValue.contains("-D%s=".formatted(entry.getKey())))
                    .map(FixIdeaRunConfigsTask::joinVmOption)
                    .collect(Collectors.joining(" "));

            if (append.isEmpty()) return;

            valueAttr.setTextContent("%s %s".formatted(currentValue, append));
        }
    }

    private void applyClassPathModifications(Document document, Node classpathModifications) {
        final Set<String> existing = new HashSet<>();

        NodeList childNodes = classpathModifications.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);

            if (!"entry".equals(child.getNodeName())) continue;

            NamedNodeMap attributes = child.getAttributes();
            Node exclude = attributes.getNamedItem("exclude");
            if (exclude == null || !"true".equals(exclude.getTextContent())) continue;

            Node pathAttr = attributes.getNamedItem("path");
            if (pathAttr != null) {
                existing.add(pathAttr.getTextContent());
            }
        }

        final Set<File> excludes = getClasspathExcludes().getFiles();

        for (File file : excludes) {
            String str = file.toString();
            if (existing.contains(str)) continue;

            Element entry = document.createElement("entry");
            entry.setAttribute("exclude", "true");
            entry.setAttribute("path", str);

            classpathModifications.appendChild(entry);
        }
    }
}
