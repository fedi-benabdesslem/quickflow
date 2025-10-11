package com.ai.application.model.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_data")
public class userData {

    @Id
    private Long id;
    private Long userId;
    private String infoKey;
    private String infoValue;

    public userData() {}

    public userData(Long id, Long userId, String infoKey, String infoValue) {
        this.id = id;
        this.userId = userId;
        this.infoKey = infoKey;
        this.infoValue = infoValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getInfoKey() {
        return infoKey;
    }

    public void setInfoKey(String infoKey) {
        this.infoKey = infoKey;
    }

    public String getInfoValue() {
        return infoValue;
    }

    public void setInfoValue(String infoValue) {
        this.infoValue = infoValue;
    }
}
