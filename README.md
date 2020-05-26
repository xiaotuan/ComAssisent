# ComAssisent
串口调试助手

##自定义命令文件格式如下：

```json
[
  {
    "name": "init",
    "commands": [
      {
        "command": "AT",
        "is_show_txt": true,
        "delay": 0
      }, {
        "command": "AT^SIMST?",
        "is_show_txt": true,
        "delay": 1000
      }, {
        "command": "AT+CGCLASS=\"A\"",
        "is_show_txt": true,
        "delay": 500
      }
    ]
  },
  {
    "name": "Dial",
    "commands": [
      {
        "command": "ATD18123786266;",
        "is_show_txt": true,
        "delay": 0
      }
    ]
  },
  {
    "name": "HandUp",
    "commands": [
      {
        "command": "AT+CHUP",
        "is_show_txt": true,
        "delay": 500
      }
    ]
  }
]
```

其中， `name` 为该命令序列的名称；`commands` 为该命令序列的命令数组；`command` 为具体命令字符；
`is_show_txt` 表示该命令是以字符串形式发送（true）还是十六进制发送（false）；`delay` 表示发送该命令前休眠时间。

命令中止符请在应用界面中进行设置。中止符是以十六进制进行输入，如果中止符包含多个字符，每个字符以空格隔开。

命令以十六进制发送时，十六进制字符不需要用间隔符隔开，但是必须保证每个十六进制字符必须是两位，例如：0D。

日志建议设置自动清除，行数尽可能设置小些，避免内存耗尽问题。
