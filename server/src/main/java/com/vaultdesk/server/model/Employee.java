package com.vaultdesk.server.model;

public record Employee(int id,String name,String empCode,int departmentId,String designation,String email,String phone,String joinDate,String leaveDate,int active,String notes) {
}
