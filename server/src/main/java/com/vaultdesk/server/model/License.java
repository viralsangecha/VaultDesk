package com.vaultdesk.server.model;

public record License(int id,String softwareName,String licenseType,String licenseKey,int seatsTotal,int seatsUsed,String vendor,String purchaseDate,String expiryDate,double cost,String notes) {
}
