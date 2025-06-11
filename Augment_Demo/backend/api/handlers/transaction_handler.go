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

type TransactionHandler struct{}

func NewTransactionHandler() *TransactionHandler {
	return &TransactionHandler{}
}

// GetTransactions returns transactions for a specific account
func (h *TransactionHandler) GetTransactions(c *gin.Context) {
	userID, exists := middleware.GetUserIDFromContext(c)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "User not authenticated",
		})
		return
	}
	
	accountIDStr := c.Param("accountId")
	accountID, err := strconv.ParseUint(accountIDStr, 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid account ID",
		})
		return
	}
	
	// Verify account belongs to user
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
	
	// Parse query parameters
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "50"))
	offset, _ := strconv.Atoi(c.DefaultQuery("offset", "0"))
	
	var transactions []models.Transaction
	query := db.DB.Where("from_account_id = ? OR to_account_id = ?", accountID, accountID).
		Order("created_at DESC").
		Limit(limit).
		Offset(offset)
	
	if err := query.Find(&transactions).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to fetch transactions",
		})
		return
	}
	
	var transactionResponses []models.TransactionResponse
	for _, transaction := range transactions {
		transactionResponses = append(transactionResponses, transaction.ToResponse())
	}
	
	c.JSON(http.StatusOK, gin.H{
		"data": transactionResponses,
	})
}

// CreateTransfer creates a transfer between accounts
func (h *TransactionHandler) CreateTransfer(c *gin.Context) {
	userID, exists := middleware.GetUserIDFromContext(c)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "User not authenticated",
		})
		return
	}
	
	fromAccountIDStr := c.Param("accountId")
	fromAccountID, err := strconv.ParseUint(fromAccountIDStr, 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid account ID",
		})
		return
	}
	
	var req models.TransferRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid request data",
			"details": err.Error(),
		})
		return
	}
	
	// Start transaction
	tx := db.DB.Begin()
	defer func() {
		if r := recover(); r != nil {
			tx.Rollback()
		}
	}()
	
	// Get from account
	var fromAccount models.Account
	if err := tx.Where("id = ? AND user_id = ?", fromAccountID, userID).First(&fromAccount).Error; err != nil {
		tx.Rollback()
		if errors.Is(err, gorm.ErrRecordNotFound) {
			c.JSON(http.StatusNotFound, gin.H{
				"error": "From account not found",
			})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Database error",
		})
		return
	}
	
	// Get to account
	var toAccount models.Account
	if err := tx.Where("account_number = ?", req.ToAccountNumber).First(&toAccount).Error; err != nil {
		tx.Rollback()
		if errors.Is(err, gorm.ErrRecordNotFound) {
			c.JSON(http.StatusNotFound, gin.H{
				"error": "Destination account not found",
			})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Database error",
		})
		return
	}
	
	// Validate transfer
	if !fromAccount.CanDebit(req.Amount) {
		tx.Rollback()
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Insufficient funds or account inactive",
		})
		return
	}
	
	if !toAccount.CanCredit() {
		tx.Rollback()
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Destination account cannot receive funds",
		})
		return
	}
	
	if fromAccount.Currency != toAccount.Currency {
		tx.Rollback()
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Currency mismatch between accounts",
		})
		return
	}
	
	// Create transaction record
	transaction := models.Transaction{
		FromAccountID:   uint(fromAccountID),
		ToAccountID:     &toAccount.ID,
		Amount:          req.Amount,
		TransactionType: models.TransactionTypeTransfer,
		Status:          models.TransactionStatusPending,
		Description:     req.Description,
		Reference:       generateTransactionReference(),
		BalanceBefore:   fromAccount.Balance,
		BalanceAfter:    fromAccount.Balance - req.Amount,
	}
	
	if err := tx.Create(&transaction).Error; err != nil {
		tx.Rollback()
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to create transaction",
		})
		return
	}
	
	// Update account balances
	if err := tx.Model(&fromAccount).Update("balance", fromAccount.Balance-req.Amount).Error; err != nil {
		tx.Rollback()
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to update from account balance",
		})
		return
	}
	
	if err := tx.Model(&toAccount).Update("balance", toAccount.Balance+req.Amount).Error; err != nil {
		tx.Rollback()
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to update to account balance",
		})
		return
	}
	
	// Mark transaction as completed
	if err := tx.Model(&transaction).Update("status", models.TransactionStatusCompleted).Error; err != nil {
		tx.Rollback()
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to complete transaction",
		})
		return
	}
	
	// Commit transaction
	if err := tx.Commit().Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to commit transaction",
		})
		return
	}
	
	c.JSON(http.StatusCreated, gin.H{
		"message": "Transfer completed successfully",
		"data":    transaction.ToResponse(),
	})
}

// GetTransaction returns a specific transaction
func (h *TransactionHandler) GetTransaction(c *gin.Context) {
	userID, exists := middleware.GetUserIDFromContext(c)
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "User not authenticated",
		})
		return
	}
	
	transactionIDStr := c.Param("id")
	transactionID, err := strconv.ParseUint(transactionIDStr, 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Invalid transaction ID",
		})
		return
	}
	
	var transaction models.Transaction
	query := db.DB.Joins("JOIN accounts ON accounts.id = transactions.from_account_id").
		Where("transactions.id = ? AND accounts.user_id = ?", transactionID, userID)
	
	if err := query.First(&transaction).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			c.JSON(http.StatusNotFound, gin.H{
				"error": "Transaction not found",
			})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Database error",
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"data": transaction.ToResponse(),
	})
}

// generateTransactionReference generates a unique transaction reference
func generateTransactionReference() string {
	rand.Seed(time.Now().UnixNano())
	return fmt.Sprintf("TXN%d%06d", time.Now().Unix(), rand.Intn(1000000))
}
