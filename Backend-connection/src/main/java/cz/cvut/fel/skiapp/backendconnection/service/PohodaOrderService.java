package cz.cvut.fel.skiapp.backendconnection.service;

import cz.cvut.fel.skiapp.backendconnection.repository.ServiceOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

@Service
@Transactional
public class PohodaOrderService {

    @Autowired
    private ServiceOrderRepository orderRepository;

    /*public ArrayList<PohodaOrder> importOrdersFromXml(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

        ArrayList<PohodaOrder> pohodaOrders = new ArrayList<>();

        // Najdeme všechny bloky <lst:order>
        NodeList orderList = doc.getElementsByTagNameNS("*", "order");

        for (int i = 0; i < orderList.getLength(); i++) {
            Element orderNode = (Element) orderList.item(i);

            // Hlavička
            Element header = (Element) orderNode.getElementsByTagNameNS("*", "orderHeader").item(0);
            if (header == null) continue;

            PohodaOrder order = new PohodaOrder();
            order.setPohodaId(Long.parseLong(getVal(header, "id")));

            // Číslo objednávky (je zanořené v ord:number)
            Element numEl = (Element) header.getElementsByTagNameNS("*", "number").item(0);
            order.setOrderNumber(getVal(numEl, "numberRequested"));

            order.setDateCreated(parseDate(getVal(header, "date")));
            order.setDateFrom(parseDate(getVal(header, "dateFrom")));
            order.setDateTo(parseDate(getVal(header, "dateTo")));
            order.setNote(getVal(header, "note"));
            order.setInternalNote(getVal(header, "intNote"));

            // Partner (Zákazník)
            Element partner = (Element) header.getElementsByTagNameNS("*", "partnerIdentity").item(0);
            Element addr = (Element) partner.getElementsByTagNameNS("*", "address").item(0);
            order.setCustomerCompany(getVal(addr, "company"));
            order.setCustomerName(getVal(addr, "name"));
            order.setCustomerCity(getVal(addr, "city"));
            order.setCustomerStreet(getVal(addr, "street"));
            order.setCustomerZip(getVal(addr, "zip"));
            order.setCustomerIco(getVal(addr, "ico"));
            order.setCustomerEmail(getVal(addr, "email"));
            order.setCustomerPhone(getVal(addr, "phone"));

            // Suma (Celková cena bez DPH z ord:homeCurrency)
            Element summary = (Element) orderNode.getElementsByTagNameNS("*", "orderSummary").item(0);
            Element homeCurr = (Element) summary.getElementsByTagNameNS("*", "homeCurrency").item(0);
            order.setTotalPrice(new BigDecimal(getVal(homeCurr, "priceNone")));

            // Položky (Produkty)
            NodeList itemNodes = orderNode.getElementsByTagNameNS("*", "orderItem");
            for (int j = 0; j < itemNodes.getLength(); j++) {
                Element itemEl = (Element) itemNodes.item(j);
                PohodaOrderItem item = new PohodaOrderItem();
                item.setProductText(getVal(itemEl, "text"));
                item.setProductCode(getVal(itemEl, "code"));
                item.setQuantity(new BigDecimal(getVal(itemEl, "quantity")));
                item.setUnit(getVal(itemEl, "unit"));

                Element itemCurr = (Element) itemEl.getElementsByTagNameNS("*", "homeCurrency").item(0);
                item.setUnitPrice(new BigDecimal(getVal(itemCurr, "unitPrice")));

                item.setOrder(order);
                order.getItems().add(item);
            }

            // Uložíme do DB (Hibernate vyřeší INSERT nebo UPDATE díky @Transactional)
            orderRepository.save(order);
            pohodaOrders.add(order);
        }

        return pohodaOrders;
    }

    private String getVal(Element el, String tag) {
        if (el == null) return null;
        NodeList nl = el.getElementsByTagNameNS("*", tag);
        if (nl.getLength() > 0) return nl.item(0).getTextContent();
        return null;
    }

    private LocalDate parseDate(String date) {
        return (date != null && !date.isEmpty()) ? LocalDate.parse(date) : null;
    }*/
}
