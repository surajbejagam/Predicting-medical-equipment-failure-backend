package com.example.predict.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "manufacturers")
@Data
public class Manufacturers {
    private String name;
    private String parent_company;
    @Id
    private int id;



}
