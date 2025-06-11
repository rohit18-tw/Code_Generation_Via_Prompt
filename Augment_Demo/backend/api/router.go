package api

import (
	"net/http"
	"time"
	
	"github.com/gin-gonic/gin"
	
	"simple-bank-backend/backend/api/handlers"
	"simple-bank-backend/backend/config"
	"simple-bank-backend/backend/db"
	"simple-bank-backend/backend/middleware"
)

func SetupRouter(config *config.Config) *gin.Engine {
	// Set Gin mode based on environment
	if config.IsProduction() {
		gin.SetMode(gin.ReleaseMode)
	}
	
	router := gin.New()
	
	// Middleware
	router.Use(gin.Logger())
	router.Use(gin.Recovery())
	router.Use(corsMiddleware())
	
	// Initialize handlers
	authHandler := handlers.NewAuthHandler(config)
	accountHandler := handlers.NewAccountHandler()
	transactionHandler := handlers.NewTransactionHandler()
	
	// Health check endpoint
	router.GET("/health", healthCheck)
	
	// API v1 routes
	v1 := router.Group("/api/v1")
	{
		// Public routes (no authentication required)
		auth := v1.Group("/auth")
		{
			auth.POST("/register", authHandler.Register)
			auth.POST("/login", authHandler.Login)
			auth.POST("/refresh", authHandler.RefreshToken)
		}
		
		// Protected routes (authentication required)
		protected := v1.Group("/")
		protected.Use(middleware.AuthMiddleware(config))
		{
			// User profile routes
			protected.GET("/profile", authHandler.GetProfile)
			
			// Account routes
			accounts := protected.Group("/accounts")
			{
				accounts.GET("", accountHandler.GetAccounts)
				accounts.POST("", accountHandler.CreateAccount)
				accounts.GET("/:id", accountHandler.GetAccount)
				accounts.PUT("/:id", accountHandler.UpdateAccount)
				accounts.DELETE("/:id", accountHandler.DeleteAccount)
				
				// Transaction routes nested under accounts
				accounts.GET("/:accountId/transactions", transactionHandler.GetTransactions)
				accounts.POST("/:accountId/transfer", transactionHandler.CreateTransfer)
			}
			
			// Transaction routes
			transactions := protected.Group("/transactions")
			{
				transactions.GET("/:id", transactionHandler.GetTransaction)
			}
		}
	}
	
	// 404 handler
	router.NoRoute(func(c *gin.Context) {
		c.JSON(http.StatusNotFound, gin.H{
			"error": "Route not found",
		})
	})
	
	return router
}

// corsMiddleware handles CORS headers
func corsMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Origin, Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")
		c.Header("Access-Control-Expose-Headers", "Content-Length")
		c.Header("Access-Control-Allow-Credentials", "true")
		
		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}
		
		c.Next()
	}
}

// healthCheck endpoint for monitoring
func healthCheck(c *gin.Context) {
	// Check database health
	dbHealth := "healthy"
	if err := db.HealthCheck(); err != nil {
		dbHealth = "unhealthy: " + err.Error()
	}
	
	status := http.StatusOK
	if dbHealth != "healthy" {
		status = http.StatusServiceUnavailable
	}
	
	c.JSON(status, gin.H{
		"status":    "ok",
		"timestamp": time.Now().UTC(),
		"database":  dbHealth,
		"version":   "1.0.0",
	})
}
