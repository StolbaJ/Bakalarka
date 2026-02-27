package cz.cvut.fel.skiapp.backendconnection.service;

import cz.cvut.fel.skiapp.backendconnection.integration.PohodaConnection;
import cz.cvut.fel.skiapp.backendconnection.model.*;
import cz.cvut.fel.skiapp.backendconnection.repository.CustomerRepository;
import cz.cvut.fel.skiapp.backendconnection.repository.OrderRepository;
import cz.cvut.fel.skiapp.backendconnection.repository.PohodaOrderRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class PohodaService {

    private CustomerRepository customerRepository;
    private PohodaOrderService pohodaOrderService;
    private PohodaOrderRepository pohodaOrderRepository;
    private PohodaConnection pohodaConnection;
    private OrderRepository orderRepository;



    @Transactional
    public void processPohodaOrders() throws Exception {

        //Get XML data from Pohoda API
        String xmlContent = pohodaConnection.getAllOrders().getBody();

        // Parse XML and create PohodaOrder objects
        ArrayList<PohodaOrder> pohodaOrders = pohodaOrderService.importOrdersFromXml(xmlContent);

        // Uložení PohodaOrderů do DB
        for (PohodaOrder pohodaOrder : pohodaOrders) {

            // 1. Najdi nebo vytvoř zákazníka
            Customer customer = customerRepository.getCustomersByEmail(pohodaOrder.getCustomerEmail())
                    .orElseGet(() -> {
                        Customer newCustomer = Customer.builder().name(pohodaOrder.getCustomerName()).
                                email(pohodaOrder.getCustomerEmail()).address(pohodaOrder.getCustomerCity() + " " + pohodaOrder.getCustomerStreet()).
                                phone(pohodaOrder.getCustomerPhone()).build();
                        return customerRepository.save(newCustomer); // Uloží se do společné tabulky
                    });

            // 2. Vytvoř objednávku a úkoly pro technika
            Order order = new Order(customer, pohodaOrder.getDateTo(), pohodaOrder.getTotalPrice(), pohodaOrder.getPohodaId(), pohodaOrder.getNote());

            Set<OrderTask> orderTasks = new LinkedHashSet<>();
            for (PohodaOrderItem item : pohodaOrder.getItems()) {
                OrderTask task = new OrderTask();
                task.setOrder(order);
                task.setSki(null); // TODO: najít lyže podle item.getProductCode() a nastavit je do tasku
                orderTasks.add(task);
            }
            //orderRepository.save(order);
        }
        // 1. Najdi nebo vytvoř zákazníka


        // V tuhle chvíli je objednávka v DB a kamarád ji hned vidí ve svém API/Frontendu
    }


    public ArrayList<Order> getNewOrders() {
        return null;
    }
}
