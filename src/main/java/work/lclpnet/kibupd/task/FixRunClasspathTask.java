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
import java.util.HashSet;
import java.util.Set;

public abstract class FixRunClasspathTask extends DefaultTask {

    @InputFiles
    public abstract ConfigurableFileCollection getInputFiles();

    @InputFiles
    public abstract ConfigurableFileCollection getClasspathExcludes();

    public FixRunClasspathTask() {
        setGroup(KibuGradlePlugin.TASK_GROUP);
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

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if ("classpathModifications".equals(child.getNodeName())) {
                classpathModifications = child;
                break;
            }
        }

        if (classpathModifications == null) {
            classpathModifications = document.createElement("classpathModifications");
            configuration.appendChild(classpathModifications);
        }

        applyClassPathModifications(document, classpathModifications);
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
