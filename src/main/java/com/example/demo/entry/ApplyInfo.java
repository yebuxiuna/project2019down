package com.example.demo.entry;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@DynamicUpdate
@Data
public class ApplyInfo {
    @Id
    int uid;
    int fid;
}