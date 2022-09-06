package com.like.ble.command

/**
 * 释放命令执行者资源的命令，只是内部使用。使用者不用。
 */
class CloseCommand : Command("释放命令执行者资源的命令", immediately = true)