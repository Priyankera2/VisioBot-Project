package com.example.projectbot

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectbot.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var imageButton: ImageView
    private var selectedImageUri: Uri? = null
    private val imageLabels = mutableMapOf<String, String>()
    private val imageResponses = mutableMapOf<String, String>()
    private val textResponses = mutableMapOf<String, String>()
    private var originalSendButtonClickListener: OnClickListener? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            messageEditText.hint = "Enter a label for the image"
        }
    }

    private fun saveTrainingData() {
        val gson = Gson()
        val trainingData = mapOf("imageLabels" to imageLabels, "imageResponses" to imageResponses, "textResponses" to textResponses)
        val json = gson.toJson(trainingData)
        openFileOutput("training_data.json", MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    private fun loadTrainingData() {
        try {
            openFileInput("training_data.json").use {
                val json = String(it.readBytes())
                val gson = Gson()
                val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                val loadedData = gson.fromJson<Map<String, Map<String, String>>>(json, type)
                if (loadedData != null) {
                    imageLabels.putAll(loadedData["imageLabels"] ?: emptyMap())
                    imageResponses.putAll(loadedData["imageResponses"] ?: emptyMap())
                    textResponses.putAll(loadedData["textResponses"] ?: emptyMap())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveChatHistory() {
        Log.d("ChatHistory", "saveChatHistory() called")
        Log.d("ChatHistory", "Messages size: ${messages.size}")
        messages.forEachIndexed { index, chatMessage ->
            Log.d("ChatHistory", "Message $index: $chatMessage")
        }
        val gson = Gson()
        val json = gson.toJson(messages)
        Log.d("ChatHistory", "JSON: $json")
        try {
            val file = File(filesDir, "chat_history.json")
            openFileOutput("chat_history.json", MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
            Log.d("ChatHistory", "Chat history saved successfully. File size: ${file.length()}")
        } catch (e: Exception) {
            Log.e("ChatHistory", "Error saving chat history: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun loadChatHistory() {
        Log.d("ChatHistory", "Loading chat history...")
        val file = File(filesDir, "chat_history.json")
        if (file.exists()) {
            try {
                openFileInput("chat_history.json").use {
                    val json = String(it.readBytes())
                    Log.d("ChatHistory", "Loaded JSON: $json")
                    val gson = Gson()
                    val type = object : TypeToken<MutableList<ChatMessage>>() {}.type
                    val loadedMessages = gson.fromJson<MutableList<ChatMessage>>(json, type)
                    if (loadedMessages != null) {
                        messages.addAll(loadedMessages)
                        Log.d("ChatHistory", "Loaded ${loadedMessages.size} messages.")
                    } else {
                        Log.d("ChatHistory", "Loaded messages are null.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatHistory", "Error loading chat history: ${e.message}")
                e.printStackTrace()
            }
        } else {
            Log.d("ChatHistory", "chat_history.json does not exist.")
        }
        Log.d("ChatHistory", "Messages size after load: ${messages.size}")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        imageButton = findViewById(R.id.imageButton)

        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        loadChatHistory()
        loadTrainingData()

        originalSendButtonClickListener = OnClickListener {
            val messageText = messageEditText.text.toString()
            if (messageText.isNotEmpty() || selectedImageUri != null) {
                if (selectedImageUri != null) {
                    val imagePath = saveImageToInternalStorage(selectedImageUri!!)
                    chatAdapter.addMessage(ChatMessage(null, imagePath, true))

                    val imageName = File(imagePath).name

                    if (imageResponses.containsKey(imageName)) {
                        chatAdapter.addMessage(ChatMessage("Bot: ${imageResponses[imageName]}", null, false))
                    } else {
                        val label = messageText
                        imageLabels[imageName] = label
                        imageResponses[label] = "Thanks for the image labeled: $label"
                        chatAdapter.addMessage(ChatMessage("Bot: ${imageResponses[label]}", null, false))
                        saveTrainingData()
                    }
                    selectedImageUri = null
                    messageEditText.hint = "Type a message"
                } else if (messageText.isNotEmpty()) {
                    chatAdapter.addMessage(ChatMessage(messageText, null, true))
                    sendGroqMessage(messageText) // Call Groq API
                    messageEditText.text.clear()
                }
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
            selectedImageUri = null
        }
        sendButton.setOnClickListener(originalSendButtonClickListener)

        imageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
        }
    }

    override fun onDestroy() {
        Log.d("MainActivity", "onDestroy() called")
        super.onDestroy()
        saveChatHistory()
        saveTrainingData()
    }

    private fun saveImageToInternalStorage(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(filesDir, "image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file.absolutePath
    }

    private fun sendGroqMessage(message: String) {
        val client = OkHttpClient()
        val gson = Gson()

        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = """
        {
            "model": "llama-3.3-70b-versatile",
            "messages": [
                {
                    "role": "user",
                    "content": "$message"
                }
            ]
        }
        """.trimIndent().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer gsk_M94YHNQQvu2Z0p4lKHjcWGdyb3FYukj3KyUeb9JkHrqytU63fBbshi") // Replace with your API key
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                val jsonResponse = gson.fromJson(responseBody, Map::class.java)

                withContext(Dispatchers.Main) {
                    val botResponse = (jsonResponse["choices"] as? List<Map<String, Any>>)?.get(0)?.get("message") as? Map<String, String>
                    val responseContent = botResponse?.get("content") ?: "Groq API error"
                    chatAdapter.addMessage(ChatMessage(responseContent, null, false))
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    chatAdapter.addMessage(ChatMessage("Error communicating with Groq.", null, false))
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }
}
