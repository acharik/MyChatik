package com.example.mychatik

import android.net.Uri
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
class VoiceMessage
    { public var id: String = ""
        public  var uri: String= ""
        public  var fromId: String= ""
        public var toId: String= ""
        public var timestamp: Long= -1
    constructor(id: String,uri: String,fromId: String,toId: String,timestamp: Long){
        this.id = id
        this.uri = uri
        this.fromId = fromId
        this.toId = toId
        this.timestamp = timestamp
    }
}
