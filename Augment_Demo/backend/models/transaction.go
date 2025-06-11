package models

import (
	"time"
	"gorm.io/gorm"
)

type TransactionType string
type TransactionStatus string

const (
	TransactionTypeDebit    TransactionType = "debit"
	TransactionTypeCredit   TransactionType = "credit"
	TransactionTypeTransfer TransactionType = "transfer"
)

const (
	TransactionStatusPending   TransactionStatus = "pending"
	TransactionStatusCompleted TransactionStatus = "completed"
	TransactionStatusFailed    TransactionStatus = "failed"
	TransactionStatusCancelled TransactionStatus = "cancelled"
)

type Transaction struct {
	ID              uint              `json:"id" gorm:"primaryKey"`
	FromAccountID   uint              `json:"from_account_id" gorm:"not null"`
	ToAccountID     *uint             `json:"to_account_id,omitempty"`
	Amount          float64           `json:"amount" gorm:"not null"`
	TransactionType TransactionType   `json:"transaction_type" gorm:"not null"`
	Status          TransactionStatus `json:"status" gorm:"default:'pending'"`
	Description     string            `json:"description"`
	Reference       string            `json:"reference" gorm:"uniqueIndex"`
	BalanceBefore   float64           `json:"balance_before"`
	BalanceAfter    float64           `json:"balance_after"`
	CreatedAt       time.Time         `json:"created_at"`
	UpdatedAt       time.Time         `json:"updated_at"`
	DeletedAt       gorm.DeletedAt    `json:"-" gorm:"index"`
	
	// Relationships
	FromAccount Account  `json:"from_account,omitempty" gorm:"foreignKey:FromAccountID"`
	ToAccount   *Account `json:"to_account,omitempty" gorm:"foreignKey:ToAccountID"`
}

type TransactionResponse struct {
	ID              uint              `json:"id"`
	FromAccountID   uint              `json:"from_account_id"`
	ToAccountID     *uint             `json:"to_account_id,omitempty"`
	Amount          float64           `json:"amount"`
	TransactionType TransactionType   `json:"transaction_type"`
	Status          TransactionStatus `json:"status"`
	Description     string            `json:"description"`
	Reference       string            `json:"reference"`
	BalanceBefore   float64           `json:"balance_before"`
	BalanceAfter    float64           `json:"balance_after"`
	CreatedAt       time.Time         `json:"created_at"`
	UpdatedAt       time.Time         `json:"updated_at"`
}

type TransactionRequest struct {
	ToAccountNumber string  `json:"to_account_number,omitempty"`
	Amount          float64 `json:"amount" binding:"required,gt=0"`
	Description     string  `json:"description"`
}

type TransactionFilter struct {
	AccountID       uint              `json:"account_id,omitempty"`
	TransactionType TransactionType   `json:"transaction_type,omitempty"`
	Status          TransactionStatus `json:"status,omitempty"`
	StartDate       *time.Time        `json:"start_date,omitempty"`
	EndDate         *time.Time        `json:"end_date,omitempty"`
	Limit           int               `json:"limit,omitempty"`
	Offset          int               `json:"offset,omitempty"`
}

func (t *Transaction) ToResponse() TransactionResponse {
	return TransactionResponse{
		ID:              t.ID,
		FromAccountID:   t.FromAccountID,
		ToAccountID:     t.ToAccountID,
		Amount:          t.Amount,
		TransactionType: t.TransactionType,
		Status:          t.Status,
		Description:     t.Description,
		Reference:       t.Reference,
		BalanceBefore:   t.BalanceBefore,
		BalanceAfter:    t.BalanceAfter,
		CreatedAt:       t.CreatedAt,
		UpdatedAt:       t.UpdatedAt,
	}
}

func (t *Transaction) IsCompleted() bool {
	return t.Status == TransactionStatusCompleted
}

func (t *Transaction) IsPending() bool {
	return t.Status == TransactionStatusPending
}
