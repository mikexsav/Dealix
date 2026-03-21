package vomatix.ru.spring_2026

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
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
    suspend fun createCarSaleSafe(
        name: String,
        managerName: String,
        managerPhone: String,
        saleTypeEnumId: Int, // ⚠️ ВАЖНО: enum ID, не текст!
        price: Int,

        managerFieldId: Long,
        phoneFieldId: Long,
        typeFieldId: Long,
        priceFieldId: Long
    ): String {

        val json = """
    [
      {
        "name": "$name",
        "custom_fields_values": [
          {
            "field_id": $managerFieldId,
            "values": [
              { "value": "$managerName" }
            ]
          },
          {
            "field_id": $phoneFieldId,
            "values": [
              { "value": "$managerPhone" }
            ]
          },
          {
            "field_id": $typeFieldId,
            "values": [
              { "enum_id": $saleTypeEnumId }
            ]
          },
          {
            "field_id": $priceFieldId,
            "values": [
              { "value": $price }
            ]
          }
        ]
      }
    ]
    """.trimIndent()

        return request("POST", "/api/v4/leads", json)
    }
    suspend fun seedDataOnce() {

        // =========================
        // 🏢 Создаём компании + сохраняем ID
        // =========================
        val companyNames = listOf(
            "BMW Центр Москва",
            "Toyota Центр Казань",
            "Mercedes Центр СПБ",
            "LADA Центр Самара"
        )

        val companyIds = mutableListOf<Long>()

        companyNames.forEach {
            val res = createCompany(it)
            val id = extractId(res)
            companyIds.add(id)
        }

        // =========================
        // 👤 Сотрудники с почтой + привязкой
        // =========================
        val employees = listOf(
            Triple("Иванов Иван", "+79990000101", "ivanov@mail.ru"),
            Triple("Петров Петр", "+79990000102", "petrov@mail.ru"),
            Triple("Сидоров Алексей", "+79990000103", "sidorov@mail.ru"),
            Triple("Кузнецов Дмитрий", "+79990000104", "kuznecov@mail.ru"),
            Triple("Смирнов Максим", "+79990000105", "smirnov@mail.ru"),
            Triple("Васильев Артем", "+79990000106", "vasiliev@mail.ru"),
            Triple("Попов Егор", "+79990000107", "popov@mail.ru"),
            Triple("Соколов Кирилл", "+79990000108", "sokolov@mail.ru"),
            Triple("Лебедев Никита", "+79990000109", "lebedev@mail.ru"),
            Triple("Козлов Илья", "+79990000110", "kozlov@mail.ru"),
            Triple("Новиков Роман", "+79990000111", "novikov@mail.ru"),
            Triple("Морозов Владислав", "+79990000112", "morozov@mail.ru"),
            Triple("Волков Андрей", "+79990000113", "volkov@mail.ru"),
            Triple("Федоров Денис", "+79990000114", "fedorov@mail.ru"),
            Triple("Орлов Тимофей", "+79990000115", "orlov@mail.ru"),
            Triple("Никитин Павел", "+79990000116", "nikitin@mail.ru"),
            Triple("Захаров Михаил", "+79990000117", "zakharov@mail.ru")
        )

        employees.forEachIndexed { index, emp ->

            val companyId = companyIds[index % companyIds.size]

            createContactFull(
                name = emp.first,
                phone = emp.second,
                email = emp.third,
                companyId = companyId
            )
        }

        // =========================
        // 🚗 Сделки
        // =========================
        val deals = listOf(
            "BMW X5 продажа",
            "Toyota Camry кредит",
            "Mercedes E-class продажа",
            "LADA Vesta продажа",
            "BMW X3 кредит",
            "Toyota RAV4 продажа",
            "Mercedes GLC кредит",
            "LADA Granta продажа"
        )

        deals.forEach {
            createLead(it)
        }
    }
    suspend fun createContactFull(
        name: String,
        phone: String,
        email: String,
        companyId: Long
    ): String {

        val json = """
    [
      {
        "name": "$name",
        "custom_fields_values": [
          {
            "field_code": "PHONE",
            "values": [{ "value": "$phone" }]
          },
          {
            "field_code": "EMAIL",
            "values": [{ "value": "$email" }]
          }
        ],
        "_embedded": {
          "companies": [
            {
              "id": $companyId
            }
          ]
        }
      }
    ]
    """.trimIndent()

        return request("POST", "/api/v4/contacts", json)
    }
    fun extractId(json: String): Long {
        val regex = """"id":(\d+)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toLong() ?: 0
    }
    suspend fun createCarDealVanilla(
        carName: String,
        saleType: String,
        price: Int,
        managerName: String,
        managerPhone: String,
        managerEmail: String,
        companyName: String
    ): String = withContext(Dispatchers.IO) {

        val contact = JSONObject().apply {
            put("first_name", managerName)
            put(
                "custom_fields_values",
                JSONArray().put(
                    JSONObject().apply {
                        put("field_code", "PHONE")
                        put(
                            "values",
                            JSONArray().put(
                                JSONObject().apply {
                                    put("enum_code", "WORK")
                                    put("value", managerPhone)
                                }
                            )
                        )
                    }
                ).put(
                    JSONObject().apply {
                        put("field_code", "EMAIL")
                        put(
                            "values",
                            JSONArray().put(
                                JSONObject().apply {
                                    put("enum_code", "WORK")
                                    put("value", managerEmail)
                                }
                            )
                        )
                    }
                )
            )
        }

        val company = JSONObject().apply {
            put("name", companyName)
        }

        val lead = JSONObject().apply {
            put("name", "$carName ($saleType)")
            put("price", price)
            put(
                "_embedded",
                JSONObject().apply {
                    put("contacts", JSONArray().put(contact))
                    put("companies", JSONArray().put(company))
                }
            )
        }

        val body = JSONArray().put(lead).toString()
        request("POST", "/api/v4/leads/complex", body)
    }

}