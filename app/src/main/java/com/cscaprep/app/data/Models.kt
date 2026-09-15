package com.cscaprep.app.data

data class User(val id: Long, val name: String, val email: String, val verified: Boolean, val plan: String, val language: String)
data class AuthResult(val token: String, val user: User)
data class Subject(val id: String, val label: String, val symbol: String, val questions: Int, val minutes: Int)
data class Exam(val id: Long, val title: String, val subject: String, val duration: Int, val count: Int, val difficulty: String)
data class Attempt(val title: String, val score: Int, val total: Int, val createdAt: String)
data class Access(val plan: String, val isPro: Boolean)
data class Bootstrap(val subjects: List<Subject>, val exams: List<Exam>, val attempts: List<Attempt>, val access: Access, val verified: Boolean)
data class Question(val id: Long, val subject: String, val topic: String, val prompt: String, val options: List<String>, val answer: Int?, val explanation: String, val principle: String)
data class ExamContent(val exam: Exam, val questions: List<Question>, val language: String)
data class Result(val score: Int, val total: Int, val percent: Int, val correct: Int, val incorrect: Int, val unanswered: Int)
class ApiException(message: String, val status: Int = 0, val code: String = "") : Exception(message)
