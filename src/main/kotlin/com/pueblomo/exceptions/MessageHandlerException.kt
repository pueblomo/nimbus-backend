package com.pueblomo.exceptions

import com.pueblomo.models.MessageType

class MessageHandlerException(action: MessageType, filePath: String, ex: Exception?) :
    RuntimeException("Error on $action with file $filePath", ex)