package com.ajayaventure.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Create-user request. Passwords are hashed server-side, never stored raw. */
public class CreateUserRequest {

    private String username;
    private String email;
    private String mobile;
    @SerializedName("first_name")
    private String firstName;
    @SerializedName("last_name")
    private String lastName;
    @SerializedName("full_name")
    private String fullName;
    private String status;
    private String password;
    @SerializedName("role_id")
    private Long roleId;
    @SerializedName("business_unit_ids")
    private List<Long> businessUnitIds;
    @SerializedName("access_level")
    private String accessLevel;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public List<Long> getBusinessUnitIds() {
        return businessUnitIds;
    }

    public void setBusinessUnitIds(List<Long> businessUnitIds) {
        this.businessUnitIds = businessUnitIds;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }
}