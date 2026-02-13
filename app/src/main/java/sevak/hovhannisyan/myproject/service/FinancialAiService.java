package sevak.hovhannisyan.myproject.service;

import androidx.lifecycle.LiveData;

import java.util.List;

import sevak.hovhannisyan.myproject.data.model.Transaction;

/**
 * Interface for AI-powered financial assistance.
 * This will be implemented with Google Gemini API integration.
 */
public interface FinancialAiService {
    
    /**
     * Analyzes spending patterns and provides insights.
     *
     * @param transactions List of transactions to analyze
     * @return AI-generated insights about spending patterns
     */
    LiveData<String> analyzeSpendingPatterns(List<Transaction> transactions);
    
    /**
     * Provides personalized financial advice based on user's transaction history.
     *
     * @param transactions List of transactions
     * @param monthlyIncome Monthly income amount
     * @return AI-generated financial advice
     */
    LiveData<String> getFinancialAdvice(List<Transaction> transactions, double monthlyIncome);
    
    /**
     * Suggests budget allocations based on spending history.
     *
     * @param transactions List of transactions
     * @return AI-generated budget suggestions
     */
    LiveData<String> suggestBudget(List<Transaction> transactions);
    
    /**
     * Answers financial questions using natural language processing.
     *
     * @param question User's financial question
     * @param transactions Relevant transactions for context
     * @return AI-generated answer
     */
    LiveData<String> answerQuestion(String question, List<Transaction> transactions);
    
    /**
     * Categorizes transactions automatically using AI.
     *
     * @param transaction Transaction to categorize
     * @return Suggested category
     */
    LiveData<String> categorizeTransaction(Transaction transaction);
}
