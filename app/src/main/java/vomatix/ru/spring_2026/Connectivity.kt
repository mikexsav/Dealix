package vomatix.ru.spring_2026

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import vomatix.ru.spring_2026.models.Lead
import vomatix.ru.spring_2026.models.LeadResponse
import java.io.IOException

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
class Connectivity(
    private val baseUrl: String,
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String
) {

    private val client = OkHttpClient()
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private val jsonMediaType = "application/json".toMediaType()

    fun setTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    private suspend fun request(
        method: String,
        endpoint: String,
        body: String? = null
    ): String = withContext(Dispatchers.IO) {

        val requestBody = body?.let {
            RequestBody.create(jsonMediaType, it)
        }

        val request = Request.Builder()
            .url("$baseUrl$endpoint")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .method(method, if (method == "GET" || method == "DELETE") null else requestBody)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}: ${response.body?.string()}")
        }

        response.body?.string() ?: "empty"
    }

    // =========================
    // LEADS
    // =========================

    suspend fun getLeads(): String =
        request("GET", "/api/v4/leads")

    suspend fun createLead(name: String): String {
        val json = """[{"name":"$name"}]"""
        return request("POST", "/api/v4/leads", json)
    }

    // =========================
    // CONTACTS
    // =========================

    suspend fun createContact(name: String, phone: String): String {
        val json = """
        [
          {
            "name": "$name",
            "custom_fields_values": [
              {
                "field_code": "PHONE",
                "values": [{ "value": "$phone" }]
              }
            ]
          }
        ]
        """.trimIndent()

        return request("POST", "/api/v4/contacts", json)
    }

    // =========================
    // COMPANIES
    // =========================

    suspend fun createCompany(name: String): String {
        val json = """[{"name":"$name"}]"""
        return request("POST", "/api/v4/companies", json)
    }

    suspend fun getCompanies(): String =
        request("GET", "/api/v4/companies")

    suspend fun linkCompanyToLead(leadId: Long, companyId: Long): String {
        val json = """
        [
          {
            "to_entity_id": $companyId,
            "to_entity_type": "companies"
          }
        ]
        """.trimIndent()

        return request("POST", "/api/v4/leads/$leadId/link", json)
    }

    // =========================
    // TASKS
    // =========================

    suspend fun createTask(text: String, leadId: Long): String {
        val json = """
        [
          {
            "text": "$text",
            "entity_id": $leadId,
            "entity_type": "leads"
          }
        ]
        """.trimIndent()

        return request("POST", "/api/v4/tasks", json)
    }

    // =========================
    // NOTES
    // =========================

    suspend fun addNote(leadId: Long, text: String): String {
        val json = """
        [
          {
            "note_type": "common",
            "params": {
              "text": "$text"
            }
          }
        ]
        """.trimIndent()

        return request("POST", "/api/v4/leads/$leadId/notes", json)
    }
    suspend fun getLeadsList(): MutableList<Lead> {
        val json = request("GET", "/api/v4/leads")

        val adapter = moshi.adapter(LeadResponse::class.java)
        val response = adapter.fromJson(json)

        return response?._embedded?.leads?.toMutableList() ?: mutableListOf()
    }
}