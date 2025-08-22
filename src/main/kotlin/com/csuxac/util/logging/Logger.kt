package com.csuxac.util.logging

import mu.KotlinLogging

/**
 * Utility logging module for CsuXac
 * 
 * Provides centralized logging configuration and utilities
 * for consistent logging across the entire system.
 */

/**
 * Get a logger instance for the current class
 */
inline fun <reified T : Any> logger() = KotlinLogging.logger(T::class.java.name)

/**
 * Get a default logger instance
 */
fun defaultLogger() = KotlinLogging.logger {}