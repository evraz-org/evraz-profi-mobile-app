package com.btsplusplus.fowallet.http

import com.yanzhenjie.andserver.annotation.Controller
import com.yanzhenjie.andserver.annotation.GetMapping

@Controller
class PageController {
    @GetMapping(path = ["/"])
    fun index(): String {
        return "/index"
    }
}