package com.example.workflow.bpmn;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApproveCartBpmnTest {

    @Test
    void cancelOrderTaskUsesDelegateExpression() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/approve-cart.bpmn"));

        assertThat(xml).contains("camunda:delegateExpression=\"${cancelOrderDelegate}\"");
        assertThat(xml).doesNotContain("camunda:expression=\"${cancelOrderDelegate}\"");
    }

    @Test
    void customerReceiptTaskKeyMatchesOrderService() throws Exception {
        Document document = parseBpmn();
        var tasks = document.getElementsByTagName("bpmn:userTask");
        boolean foundTask = false;

        for (int i = 0; i < tasks.getLength(); i++) {
            var task = tasks.item(i);
            if ("customer_confirm_receipt".equals(task.getAttributes().getNamedItem("id").getNodeValue())) {
                foundTask = true;
                break;
            }
        }

        assertThat(foundTask).isTrue();
    }

    private Document parseBpmn() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/approve-cart.bpmn"))) {
            return factory.newDocumentBuilder().parse(new InputSource(reader));
        }
    }
}
