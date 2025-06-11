package utils

import (
	"errors"
	"golang.org/x/crypto/bcrypt"
)

const (
	MinPasswordLength = 6
	MaxPasswordLength = 128
	DefaultCost       = bcrypt.DefaultCost
)

var (
	ErrPasswordTooShort = errors.New("password is too short")
	ErrPasswordTooLong  = errors.New("password is too long")
	ErrInvalidPassword  = errors.New("invalid password")
)

// HashPassword hashes a plain text password using bcrypt
func HashPassword(password string) (string, error) {
	if err := ValidatePassword(password); err != nil {
		return "", err
	}
	
	hashedBytes, err := bcrypt.GenerateFromPassword([]byte(password), DefaultCost)
	if err != nil {
		return "", err
	}
	
	return string(hashedBytes), nil
}

// CheckPassword compares a hashed password with a plain text password
func CheckPassword(hashedPassword, password string) error {
	return bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password))
}

// ValidatePassword validates password requirements
func ValidatePassword(password string) error {
	if len(password) < MinPasswordLength {
		return ErrPasswordTooShort
	}
	
	if len(password) > MaxPasswordLength {
		return ErrPasswordTooLong
	}
	
	return nil
}

// IsPasswordValid checks if a password meets the requirements
func IsPasswordValid(password string) bool {
	return ValidatePassword(password) == nil
}

// GenerateRandomPassword generates a random password (for testing purposes)
func GenerateRandomPassword(length int) string {
	if length < MinPasswordLength {
		length = MinPasswordLength
	}
	
	const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*"
	password := make([]byte, length)
	
	for i := range password {
		password[i] = charset[i%len(charset)]
	}
	
	return string(password)
}
