package com.example.cabinetlib.comm;

public interface CabinetOrder_Cb {

    int query_instruct = 0x60;//Android发送查询命令
    int erase_instruct = 0x62;//Android发送擦除命令
    int write_instruct = 0x64;//Android发送写入命令
}
