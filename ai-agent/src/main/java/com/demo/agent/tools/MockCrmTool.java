package com.demo.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component("crmLookup")
@Description("Query internal company CRM and ERP system for customer profiles, order delivery tracking, or product inventory")
public class MockCrmTool implements Function<MockCrmTool.Request, MockCrmTool.Response> {

    private static final Logger log = LoggerFactory.getLogger(MockCrmTool.class);

    private final Map<String, CustomerRecord> customers = new ConcurrentHashMap<>();
    private final Map<String, OrderRecord> orders = new ConcurrentHashMap<>();
    private final Map<String, InventoryRecord> inventory = new ConcurrentHashMap<>();

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("Lookup category: 'customer', 'order', or 'inventory'")
            String category,

            @JsonProperty(required = true)
            @JsonPropertyDescription("Identifier to query (e.g. 'CUST-8812', 'ORD-1042', or 'PROD-AI-EDGE')")
            String id
    ) {}

    public record Response(String result) {}

    public record CustomerRecord(String customerId, String name, String email, String tier, double creditBalance) {}
    public record OrderRecord(String orderId, String customerId, String itemSku, String status, String trackingNumber, String estimatedDelivery) {}
    public record InventoryRecord(String sku, String name, int stockAvailable, double unitPriceUsd, String warehouseLocation) {}

    public MockCrmTool() {
        // Sample Customer Records
        customers.put("CUST-8812", new CustomerRecord("CUST-8812", "Alice Johnson", "alice.j@example.com", "Platinum VIP", 450.00));
        customers.put("CUST-5521", new CustomerRecord("CUST-5521", "Robert Smith", "rsmith@company.org", "Standard", 0.00));
        customers.put("CUST-9903", new CustomerRecord("CUST-9903", "Elena Rostova", "elena@techcorp.io", "Enterprise Gold", 1280.50));

        // Sample Orders
        orders.put("ORD-1042", new OrderRecord("ORD-1042", "CUST-8812", "PROD-AI-EDGE", "Shipped in Transit", "TRK-98374211US", "2026-09-12"));
        orders.put("ORD-2089", new OrderRecord("ORD-2089", "CUST-5521", "PROD-SERVER-RACK", "Processing at Warehouse", "Pending", "2026-09-15"));
        orders.put("ORD-3001", new OrderRecord("ORD-3001", "CUST-9903", "PROD-DEV-STATION", "Delivered", "TRK-11223344US", "2026-09-02"));

        // Sample Inventory
        inventory.put("PROD-AI-EDGE", new InventoryRecord("PROD-AI-EDGE", "Neural Compute Edge Accelerator 400W", 42, 899.99, "Warehouse West - Bay 4"));
        inventory.put("PROD-SERVER-RACK", new InventoryRecord("PROD-SERVER-RACK", "Enterprise High-Density 42U Server Rack", 15, 2499.00, "Warehouse Central - Bay 12"));
        inventory.put("PROD-DEV-STATION", new InventoryRecord("PROD-DEV-STATION", "Ultra-Workstation Xeon 64-Core", 8, 4999.00, "Warehouse East - Bay 2"));
    }

    @Override
    public Response apply(Request request) {
        String category = request.category();
        String id = request.id();
        log.info("Executing MockCrmTool query. Category: {}, ID: {}", category, id);

        if (category == null || id == null) {
            return new Response("Please provide both category ('customer', 'order', or 'inventory') and an ID.");
        }

        String catClean = category.trim().toLowerCase();
        String idClean = id.trim().toUpperCase();

        String result = switch (catClean) {
            case "customer" -> {
                CustomerRecord c = customers.get(idClean);
                if (c == null) {
                    yield "Customer not found for ID: " + idClean + ". Available sample IDs: CUST-8812, CUST-5521, CUST-9903.";
                }
                yield String.format("Customer Profile [%s]:\n- Name: %s\n- Email: %s\n- Membership Tier: %s\n- Store Credit Balance: $%.2f",
                        c.customerId(), c.name(), c.email(), c.tier(), c.creditBalance());
            }
            case "order" -> {
                OrderRecord o = orders.get(idClean);
                if (o == null) {
                    yield "Order not found for ID: " + idClean + ". Available sample IDs: ORD-1042, ORD-2089, ORD-3001.";
                }
                yield String.format("Order Status [%s]:\n- Customer ID: %s\n- Product SKU: %s\n- Status: %s\n- Tracking Number: %s\n- Estimated Delivery: %s",
                        o.orderId(), o.customerId(), o.itemSku(), o.status(), o.trackingNumber(), o.estimatedDelivery());
            }
            case "inventory" -> {
                InventoryRecord inv = inventory.get(idClean);
                if (inv == null) {
                    yield "Inventory SKU not found for ID: " + idClean + ". Available sample SKUs: PROD-AI-EDGE, PROD-SERVER-RACK, PROD-DEV-STATION.";
                }
                yield String.format("Inventory Record [%s]:\n- Item Name: %s\n- In-Stock Units: %d\n- Unit Price: $%.2f\n- Location: %s",
                        inv.sku(), inv.name(), inv.stockAvailable(), inv.unitPriceUsd(), inv.warehouseLocation());
            }
            default -> "Unknown CRM category: '" + category + "'. Supported categories: 'customer', 'order', 'inventory'.";
        };

        return new Response(result);
    }
}
