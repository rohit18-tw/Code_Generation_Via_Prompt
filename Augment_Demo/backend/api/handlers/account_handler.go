package handlers

import (
	"net/http"
	"strconv"
	"errors"
	"fmt"
	"time"
	"math/rand"
	
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	
	"simple-bank-backend/backend/db"
	"simple-bank-backend/backend/models"
	"simple-bank-backend/backend/middleware"
)

type AccountHandler struct{}

func NewAccountHandler() *AccountHandler {
	return &AccountHandler{}
}

// GetAccounts returns all accounts for the authenticated user
func (h *AccountHandler) GetAccounts(c *gin.Context) {
	userID, exists := middleware.GetUserIDFromContext(c)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "User not authenticated",
		})
		return
	}
	
	var accounts []models.Account
	if err := db.DB.Where("user_id = ?", userID).Find(&accounts).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to fetch accounts",
		})
		return
	}
	
	var accountResponses []models.AccountResponse
	for _, account := range accounts {
		accountResponses = append(accountResponses, account.ToResponse())
	}
	
	c.JSON(http.StatusOK, gin.H{
		"data": accountResponses,
	})
}

// GetAccount returns a specific account by ID
func (h *AccountHandler) GetAccount(c *gin.Context) {
	userID, exists := middleware.GetUserIDFromContext(c)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "User not authenticated",
		})
		return
	}
	
	accountIDStr := c.Param("id")
	accountID, err := strconv.ParseUint(accountIDStr, 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid account ID",
		})
		return
	}
	
	var account models.Account
	if err := db.DB.Where("id = ? AND user_id = ?", accountID, userID).First(&account).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			c.JSON(http.StatusNotFound, gin.H{
				"error": "Account not found",
			})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Database error",
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"data": account.ToResponse(),
	})
}

// CreateAccount creates a new account for the authenticated user
func (h *AccountHandler) CreateAccount(c *gin.Context) {
	userID, exists := middleware.GetUserIDFromContext(c)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "User not authenticated",
		})
		return
	}
	
	var req models.CreateAccountRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid request data",
			"details": err.Error(),
		})
		return
	}
	
	// Generate unique account number
	accountNumber := generateAccountNumber()
	
	// Ensure account number is unique
	for {
		var existingAccount models.Account
		if err := db.DB.Where("account_number = ?", accountNumber).First(&existingAccount).Error; err != nil {
			if errors.Is(err, gorm.ErrRecordNotFound) {
				break // Account number is unique
			}
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": "Database error",
			})
			return
		}
		accountNumber = generateAccountNumber()
	}
	
	account := models.Account{
		UserID:        userID,
		AccountNumber: accountNumber,
		AccountType:   req.AccountType,
		Balance:       0.0,
		Currency:      req.Currency,
		IsActive:      true,
	}
	
	if err := db.DB.Create(&account).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to create account",
		})
		return
	}
	
	c.JSON(http.StatusCreated, gin.H{
		"message": "Account created successfully",
		"data":    account.ToResponse(),
	})
}

// UpdateAccount updates an existing account
func (h *AccountHandler) UpdateAccount(c *gin.Context) {
	userID, exists := middleware.GetUserIDFromContext(c)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "User not authenticated",
		})
		return
	}
	
	accountIDStr := c.Param("id")
	accountID, err := strconv.ParseUint(accountIDStr, 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid account ID",
		})
		return
	}
	
	var req models.UpdateAccountRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid request data",
			"details": err.Error(),
		})
		return
	}
	
	var account models.Account
	if err := db.DB.Where("id = ? AND user_id = ?", accountID, userID).First(&account).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			c.JSON(http.StatusNotFound, gin.H{
				"error": "Account not found",
			})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Database error",
		})
		return
	}
	
	// Update fields if provided
	if req.IsActive != nil {
		account.IsActive = *req.IsActive
	}
	
	if err := db.DB.Save(&account).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to update account",
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"message": "Account updated successfully",
		"data":    account.ToResponse(),
	})
}

// DeleteAccount soft deletes an account
func (h *AccountHandler) DeleteAccount(c *gin.Context) {
	userID, exists := middleware.GetUserIDFromContext(c)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "User not authenticated",
		})
		return
	}
	
	accountIDStr := c.Param("id")
	accountID, err := strconv.ParseUint(accountIDStr, 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid account ID",
		})
		return
	}
	
	var account models.Account
	if err := db.DB.Where("id = ? AND user_id = ?", accountID, userID).First(&account).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			c.JSON(http.StatusNotFound, gin.H{
				"error": "Account not found",
			})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Database error",
		})
		return
	}
	
	// Check if account has balance
	if account.Balance > 0 {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Cannot delete account with positive balance",
		})
		return
	}
	
	if err := db.DB.Delete(&account).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to delete account",
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"message": "Account deleted successfully",
	})
}

// generateAccountNumber generates a random account number
func generateAccountNumber() string {
	rand.Seed(time.Now().UnixNano())
	return fmt.Sprintf("%010d", rand.Intn(10000000000))
}
