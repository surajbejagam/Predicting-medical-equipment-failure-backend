package com.example.predict.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "events")
@Data
public class Events {
    @Id
    private int  id;
    private String name;
    private String description;
    private String status;
    private  String action;
    private  String date;
    private int device_id;
}

