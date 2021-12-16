package com.example.mychatik

import android.annotation.SuppressLint
import android.media.MediaRecorder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import java.io.File
import java.util.*


class ChatLogActivity : AppCompatActivity() {

    companion object {
        val TAG = "ChatLog"

    }

    val adapter = GroupAdapter<ViewHolder>()

    var toUser: User? = null
    var fileg: File? = null
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)
        send_voice_button_chat_log2.visibility = View.GONE
        recyclerview_chat_log.adapter = adapter

        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)

        supportActionBar?.title = toUser?.username

        //      val username = intent.getStringExtra(NewMessageActivity.USER_KEY)

        //     val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        //     supportActionBar?.title = toUser?.username

        val adapter = GroupAdapter<ViewHolder>()

        listenForMessages()
        //       setupDummyData()

        edittext_chat_log.setOnClickListener {
            Log.d(TAG, "Clicking edit text")

            //    edittext_chat_log.visibility = View.GONE


            //    val anim: Animation =
            //     AnimationUtils.loadAnimation(
            //       this,
            //    )
            //    edittext_chat_log.startAnimation(anim)
        }

        send_button_chat_log.setOnClickListener {
            Log.d(TAG, "Attempt to send message")
            performSendMessage()

        }
        send_voice_button_chat_log.setOnTouchListener { v, event ->
            if (checkPermission(RECORD_AUDIO)) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    edittext_chat_log.setHint("Recording")
                    send_voice_button_chat_log.visibility = View.GONE
                    send_voice_button_chat_log2.visibility = View.VISIBLE
                    startRecord()

                    // rerw
                } else if (event.action == MotionEvent.ACTION_UP) {
                    //rew
                    edittext_chat_log.setHint("Enter Message")
                    send_voice_button_chat_log.visibility = View.VISIBLE
                    send_voice_button_chat_log2.visibility = View.GONE
                    stopRecord {  file ->
                        uploadVoiceToFirebaseStorage(Uri.fromFile(file))
                        fileg = file
                    }

                    val uri = Uri.fromFile(fileg)
                    val fromId = FirebaseAuth.getInstance().uid
                    val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
                    val toId = user?.uid
//        val reference = FirebaseDatabase.getInstance().getReference("/messages").push()
                    val reference =
                        FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()

                    val toReference =
                        FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

                    val voiceMessage = toId?.let {
                        if (fromId != null) {
                            VoiceMessage(
                                reference.key!!, uri.toString(), fromId,
                                it, System.currentTimeMillis() / 1000
                            )
                        }
                    }
                    reference.setValue(voiceMessage)
                        .addOnSuccessListener {
                            Log.d(TAG, "Saved our chat message: ${reference.key}")
                            edittext_chat_log.text.clear()
                            recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                        }

                    toReference.setValue(voiceMessage)

                }
            }
            true
        }
    }

    private fun listenForMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)

                if (chatMessage != null) {
                    Log.d(TAG, chatMessage.text)

                    if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                        val currentUser = LatestMessagesActivity.currentUser ?: return
                        adapter.add(ChatFromitem(chatMessage.text, currentUser))

                    } else {
                        adapter.add(ChatToitem(chatMessage.text, toUser!!))
                    }
                }

                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)


            }

            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }
        })
    }


    private fun performSendMessage() {
        //how do we actually send a message to firebase....
        val text = edittext_chat_log.text.toString()
        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user?.uid

        if (fromId == null) return

//        val reference = FirebaseDatabase.getInstance().getReference("/messages").push()
        val reference =
            FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()

        val toReference =
            FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessage = toId?.let {
            ChatMessage(
                reference.key!!, text, fromId,
                it, System.currentTimeMillis() / 1000
            )
        }
        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved our chat message: ${reference.key}")
                edittext_chat_log.text.clear()
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

        toReference.setValue(chatMessage)

        val latestMessageRef = FirebaseDatabase.getInstance().getReference(
            "/latest-messages/" +
                    "$fromId/$toId"
        )
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference(
            "/latest-messages/" +
                    "$toId/$fromId"
        )
        latestMessageToRef.setValue(chatMessage)
    }

    private fun setupDummyData() {

        val adapter = GroupAdapter<ViewHolder>()
//        adapter.add(ChatFromitem("FROM MESSSSSSAGEEEE"))
//        adapter.add(ChatToitem("TO MESSAGE\n TO MESSAGE"))
//        adapter.add(ChatFromitem("FROM MESSSSSSAGEEEE"))
//        adapter.add(ChatToitem("TO MESSAGE\n TO MESSAGE"))
//        adapter.add(ChatFromitem("FROM MESSSSSSAGEEEE"))
//        adapter.add(ChatToitem("TO MESSAGE\n TO MESSAGE"))
//        adapter.add(ChatFromitem("FROM MESSSSSSAGEEEE"))
//        adapter.add(ChatToitem("TO MESSAGE\n TO MESSAGE"))
//        adapter.add(ChatFromitem("FROM MESSSSSSAGEEEE"))
//        adapter.add(ChatToitem("TO MESSAGE\n TO MESSAGE"))

        recyclerview_chat_log.adapter = adapter

    }

    private fun uploadVoiceToFirebaseStorage(selectVoiceUri: Uri) {

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/voices/${filename}")
        ref.putFile(selectVoiceUri)
            .addOnSuccessListener {
                Log.d("ChatLog", "Successfully uploaded voice: ${it.metadata?.path}")
                ref.downloadUrl.addOnSuccessListener {
                    Log.d("ChatLog", "File Location: $it")

                }
                    .addOnFailureListener {
                        Log.d("ChatLog", "Failed to uploaded voice to storage: ${it.message} ")
                    }
            }
    }
    private val mMediaRecorder = MediaRecorder()
    private lateinit var mFile: File
    fun startRecord() {
        try {
            createFileForRecord()
            prepareMediaRecorder()
            mMediaRecorder.start()
        } catch (e: Exception) {
            Toast.makeText(APP_ACTIVITY, "q1 ${e.message.toString()}", Toast.LENGTH_SHORT)
                .show()

        }

    }

    private fun prepareMediaRecorder() {
        mMediaRecorder.apply {
            reset()
            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            setOutputFile(mFile.absolutePath)
            prepare()
        }

    }

    private fun createFileForRecord(): File {
        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user?.uid
        mFile = File(APP_ACTIVITY.filesDir,"/voices")
        mFile.createNewFile()
        return mFile
    }

    fun stopRecord(onSuccess: (file: File) -> Unit) {
        try {
            mMediaRecorder.stop()
            onSuccess(mFile)
        } catch (e: Exception) {
            Toast.makeText(APP_ACTIVITY,"q2 ${e.message.toString()}", Toast.LENGTH_SHORT)
                .show()
            mFile.delete()
        }
    }

    fun releaseRecorder() {
        try {
            mMediaRecorder.release()
        } catch (e: Exception) {
            Toast.makeText(APP_ACTIVITY,"q3 ${e.message.toString()}", Toast.LENGTH_SHORT)
                .show()
        }
    }
   /* private fun performSendVoice() {
        //how do we actually send a message to firebase....
        val uri = Uri.fromFile(file)
        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user?.uid

        if (fromId == null) return

//        val reference = FirebaseDatabase.getInstance().getReference("/messages").push()
        val reference =
            FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()

        val toReference =
            FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val voiceMessage = toId?.let {
            VoiceMessage(
                reference.key!!, uri, fromId,
                it, System.currentTimeMillis() / 1000
            )
        }
        reference.setValue(voiceMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved our chat message: ${reference.key}")
                edittext_chat_log.text.clear()
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

        toReference.setValue(voiceMessage)
    }*/

}