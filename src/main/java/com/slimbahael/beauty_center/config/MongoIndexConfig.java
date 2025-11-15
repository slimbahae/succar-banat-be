package com.slimbahael.beauty_center.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class MongoIndexConfig implements CommandLineRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Create indexes for User collection
        IndexOperations userIndexOps = mongoTemplate.indexOps("users");
        IndexDefinition emailIndex = new Index().on("email", org.springframework.data.domain.Sort.Direction.ASC).unique();
        IndexDefinition roleIndex = new Index().on("role", org.springframework.data.domain.Sort.Direction.ASC);


        // Create indexes for Product collection
        IndexOperations productIndexOps = mongoTemplate.indexOps("products");
        IndexDefinition productNameIndex = new Index().on("name", org.springframework.data.domain.Sort.Direction.ASC);
        IndexDefinition productCategoryIndex = new Index().on("category", org.springframework.data.domain.Sort.Direction.ASC);


        // Create indexes for Service collection
        IndexOperations serviceIndexOps = mongoTemplate.indexOps("services");
        IndexDefinition serviceNameIndex = new Index().on("name", org.springframework.data.domain.Sort.Direction.ASC);
        IndexDefinition serviceCategoryIndex = new Index().on("category", org.springframework.data.domain.Sort.Direction.ASC);

        // Create indexes for Reservation collection
        IndexOperations reservationIndexOps = mongoTemplate.indexOps("reservations");
        IndexDefinition customerIdIndex = new Index().on("customerId", org.springframework.data.domain.Sort.Direction.ASC);
        IndexDefinition staffIdIndex = new Index().on("staffId", org.springframework.data.domain.Sort.Direction.ASC);
        IndexDefinition reservationDateIndex = new Index().on("reservationDate", org.springframework.data.domain.Sort.Direction.ASC);

        // Create compound index for staff availability check
        IndexDefinition staffAvailabilityIndex = new Index()
                .on("staffId", org.springframework.data.domain.Sort.Direction.ASC)
                .on("reservationDate", org.springframework.data.domain.Sort.Direction.ASC)
                .on("timeSlot", org.springframework.data.domain.Sort.Direction.ASC);

        // Create indexes for Order collection
        IndexOperations orderIndexOps = mongoTemplate.indexOps("orders");
        IndexDefinition orderCustomerIndex = new Index().on("customerId", org.springframework.data.domain.Sort.Direction.ASC);
        IndexDefinition orderStatusIndex = new Index().on("orderStatus", org.springframework.data.domain.Sort.Direction.ASC);
        IndexDefinition paymentStatusIndex = new Index().on("paymentStatus", org.springframework.data.domain.Sort.Direction.ASC);
        IndexDefinition createdAtIndex = new Index().on("createdAt", org.springframework.data.domain.Sort.Direction.DESC);

        // Create indexes for Cart collection
        IndexOperations cartIndexOps = mongoTemplate.indexOps("carts");
        IndexDefinition cartCustomerIndex = new Index().on("customerId", org.springframework.data.domain.Sort.Direction.ASC).unique();
    }
}