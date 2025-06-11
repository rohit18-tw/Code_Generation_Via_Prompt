package middleware

import (
	"net/http"
	"strings"
	
	"github.com/gin-gonic/gin"
	"simple-bank-backend/backend/config"
	"simple-bank-backend/backend/utils"
)

// AuthMiddleware validates JWT tokens
func AuthMiddleware(config *config.Config) gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.JSON(http.StatusUnauthorized, gin.H{
				"error": "Authorization header is required",
			})
			c.Abort()
			return
		}
		
		// Check if the header starts with "Bearer "
		if !strings.HasPrefix(authHeader, "Bearer ") {
			c.JSON(http.StatusUnauthorized, gin.H{
				"error": "Invalid authorization header format",
			})
			c.Abort()
			return
		}
		
		// Extract the token
		tokenString := strings.TrimPrefix(authHeader, "Bearer ")
		if tokenString == "" {
			c.JSON(http.StatusUnauthorized, gin.H{
				"error": "Token is required",
			})
			c.Abort()
			return
		}
		
		// Validate the token
		claims, err := utils.ValidateToken(tokenString, config)
		if err != nil {
			var statusCode int
			var message string
			
			switch err {
			case utils.ErrExpiredToken:
				statusCode = http.StatusUnauthorized
				message = "Token has expired"
			case utils.ErrInvalidToken:
				statusCode = http.StatusUnauthorized
				message = "Invalid token"
			case utils.ErrTokenClaims:
				statusCode = http.StatusUnauthorized
				message = "Invalid token claims"
			default:
				statusCode = http.StatusUnauthorized
				message = "Token validation failed"
			}
			
			c.JSON(statusCode, gin.H{
				"error": message,
			})
			c.Abort()
			return
		}
		
		// Set user information in context
		c.Set("user_id", claims.UserID)
		c.Set("user_email", claims.Email)
		c.Set("user_first_name", claims.FirstName)
		c.Set("user_last_name", claims.LastName)
		c.Set("claims", claims)
		
		c.Next()
	}
}

// OptionalAuthMiddleware validates JWT tokens but doesn't require them
func OptionalAuthMiddleware(config *config.Config) gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.Next()
			return
		}
		
		if !strings.HasPrefix(authHeader, "Bearer ") {
			c.Next()
			return
		}
		
		tokenString := strings.TrimPrefix(authHeader, "Bearer ")
		if tokenString == "" {
			c.Next()
			return
		}
		
		claims, err := utils.ValidateToken(tokenString, config)
		if err == nil {
			c.Set("user_id", claims.UserID)
			c.Set("user_email", claims.Email)
			c.Set("user_first_name", claims.FirstName)
			c.Set("user_last_name", claims.LastName)
			c.Set("claims", claims)
		}
		
		c.Next()
	}
}

// GetUserIDFromContext extracts user ID from gin context
func GetUserIDFromContext(c *gin.Context) (uint, bool) {
	userID, exists := c.Get("user_id")
	if !exists {
		return 0, false
	}
	
	id, ok := userID.(uint)
	return id, ok
}

// GetUserEmailFromContext extracts user email from gin context
func GetUserEmailFromContext(c *gin.Context) (string, bool) {
	email, exists := c.Get("user_email")
	if !exists {
		return "", false
	}
	
	emailStr, ok := email.(string)
	return emailStr, ok
}
