package com.mock.uidai.dto;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;

/**
* Data Transfer Object for KYC (Know Your Customer) data.
* This class represents the KYC information retrieved from the UIDAI system.
*/
@Validated
public class KycDataDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar number must be 12 digits")
    private String aadhaarNumber;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @NotBlank(message = "Date of birth is required")
    @Pattern(regexp = "^\\d{2}/\\d{2}/\\d{4}$", message = "Date of birth must be in DD/MM/YYYY format")
    private String dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(M|F|O)$", message = "Gender must be M, F, or O")
    private String gender;

    @NotBlank(message = "Address is required")
    @Size(min = 1, max = 500, message = "Address must be between 1 and 500 characters")
    private String address;

    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;

    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Email must be valid")
    private String email;

    private String photo;

    /**
    * Default constructor
    */
    public KycDataDto() {
    }

    /**
    * Parameterized constructor with all fields
    */
    public KycDataDto(String aadhaarNumber, String name, String dateOfBirth, String gender, String address,
    String pincode, String phoneNumber, String email, String photo) {
        this.aadhaarNumber = aadhaarNumber;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.address = address;
        this.pincode = pincode;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.photo = photo;
    }

    /**
    * Get the Aadhaar number
    * @return the Aadhaar number
    */
    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    /**
    * Set the Aadhaar number
    * @param aadhaarNumber the Aadhaar number to set
    */
    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    /**
    * Get the name
    * @return the name
    */
    public String getName() {
        return name;
    }

    /**
    * Set the name
    * @param name the name to set
    */
    public void setName(String name) {
        this.name = name;
    }

    /**
    * Get the date of birth
    * @return the date of birth
    */
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    /**
    * Set the date of birth
    * @param dateOfBirth the date of birth to set
    */
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
    * Get the gender
    * @return the gender
    */
    public String getGender() {
        return gender;
    }

    /**
    * Set the gender
    * @param gender the gender to set
    */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
    * Get the address
    * @return the address
    */
    public String getAddress() {
        return address;
    }

    /**
    * Set the address
    * @param address the address to set
    */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
    * Get the pincode
    * @return the pincode
    */
    public String getPincode() {
        return pincode;
    }

    /**
    * Set the pincode
    * @param pincode the pincode to set
    */
    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    /**
    * Get the phone number
    * @return the phone number
    */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
    * Set the phone number
    * @param phoneNumber the phone number to set
    */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
    * Get the email
    * @return the email
    */
    public String getEmail() {
        return email;
    }

    /**
    * Set the email
    * @param email the email to set
    */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
    * Get the photo (base64 encoded)
    * @return the photo
    */
    public String getPhoto() {
        return photo;
    }

    /**
    * Set the photo (base64 encoded)
    * @param photo the photo to set
    */
    public void setPhoto(String photo) {
        this.photo = photo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KycDataDto that = (KycDataDto) o;
        return Objects.equals(aadhaarNumber, that.aadhaarNumber) &&
        Objects.equals(name, that.name) &&
        Objects.equals(dateOfBirth, that.dateOfBirth) &&
        Objects.equals(gender, that.gender) &&
        Objects.equals(address, that.address) &&
        Objects.equals(pincode, that.pincode) &&
        Objects.equals(phoneNumber, that.phoneNumber) &&
        Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aadhaarNumber, name, dateOfBirth, gender, address, pincode, phoneNumber, email);
    }

    @Override
    public String toString() {
        // Masking PII data for security
        return "KycDataDto{" +
        "aadhaarNumber='" + (aadhaarNumber != null ? "XXXX-XXXX-" + aadhaarNumber.substring(8) : null) + '\'' +
        ", name='" + (name != null ? name.charAt(0) + "***" : null) + '\'' +
        ", dateOfBirth='" + dateOfBirth + '\'' +
        ", gender='" + gender + '\'' +
        ", address='" + (address != null ? "***" : null) + '\'' +
        ", pincode='" + pincode + '\'' +
        ", phoneNumber='" + (phoneNumber != null ? "XXXXXX" + phoneNumber.substring(6) : null) + '\'' +
        ", email='" + (email != null ? email.charAt(0) + "***@" + (email.contains("@") ? email.substring(email.indexOf('@') + 1) : "") : null) + '\'' +
        ", photo='" + (photo != null ? "[PHOTO_DATA_PRESENT]" : null) + '\'' +
        '}';
    }
}