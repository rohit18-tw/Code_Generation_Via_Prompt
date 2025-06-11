package models

import (
	"time"
	"gorm.io/gorm"
)

type AccountType string

const (
	CheckingAccount AccountType = "checking"
	SavingsAccount  AccountType = "savings"
	CreditAccount   AccountType = "credit"
)

type Account struct {
	ID            uint           `json:"id" gorm:"primaryKey"`
	UserID        uint           `json:"user_id" gorm:"not null"`
	AccountNumber string         `json:"account_number" gorm:"uniqueIndex;not null"`
	AccountType   AccountType    `json:"account_type" gorm:"not null"`
	Balance       float64        `json:"balance" gorm:"default:0"`
	Currency      string         `json:"currency" gorm:"default:'USD'"`
	IsActive      bool           `json:"is_active" gorm:"default:true"`
	CreatedAt     time.Time      `json:"created_at"`
	UpdatedAt     time.Time      `json:"updated_at"`
	DeletedAt     gorm.DeletedAt `json:"-" gorm:"index"`
	
	// Relationships
	User         User          `json:"user,omitempty" gorm:"foreignKey:UserID"`
	Transactions []Transaction `json:"transactions,omitempty" gorm:"foreignKey:FromAccountID"`
}

type AccountResponse struct {
	ID            uint        `json:"id"`
	AccountNumber string      `json:"account_number"`
	AccountType   AccountType `json:"account_type"`
	Balance       float64     `json:"balance"`
	Currency      string      `json:"currency"`
	IsActive      bool        `json:"is_active"`
	CreatedAt     time.Time   `json:"created_at"`
	UpdatedAt     time.Time   `json:"updated_at"`
}

type CreateAccountRequest struct {
	AccountType AccountType `json:"account_type" binding:"required"`
	Currency    string      `json:"currency" binding:"required"`
}

type UpdateAccountRequest struct {
	IsActive *bool `json:"is_active,omitempty"`
}

type TransferRequest struct {
	ToAccountNumber string  `json:"to_account_number" binding:"required"`
	Amount          float64 `json:"amount" binding:"required,gt=0"`
	Description     string  `json:"description"`
}

func (a *Account) ToResponse() AccountResponse {
	return AccountResponse{
		ID:            a.ID,
		AccountNumber: a.AccountNumber,
		AccountType:   a.AccountType,
		Balance:       a.Balance,
		Currency:      a.Currency,
		IsActive:      a.IsActive,
		CreatedAt:     a.CreatedAt,
		UpdatedAt:     a.UpdatedAt,
	}
}

func (a *Account) CanDebit(amount float64) bool {
	return a.IsActive && a.Balance >= amount
}

func (a *Account) CanCredit() bool {
	return a.IsActive
}
