package com.telltale.kakaopay.sprinkle.exception

import com.telltale.kakaopay.sprinkle.model.ErrorResponse

class SprinkleException(val errorResponse: ErrorResponse) : Exception(errorResponse.message)
