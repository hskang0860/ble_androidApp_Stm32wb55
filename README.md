# ble_androidApp_Stm32wb55

The pupose of this software is transfer(download) file on a smartphoe to Nucleo Pack Board flash-memory at Start addr:0x0800 7000 using bluetooth 

1. you first install android-studio and Iar Compiler to compiling and download at andorid-smartphone and Stm32wb55 nucleo pack board
2. after preparing STM32WB55 Nucleo Pack Board, you compile stm32wb55(Stm32WB55_BLE_OTA/BLE_Ota/EWARM/Project.eww) and download Stm32wb55 Nucleo Pack board
3. Open andrioid BLE application and push the connect button (you have to prepare in advance that pairing to smart-phone and Stm32wb55 Nucleo Pack(GATT Server name STM_OTA, bluetooth pin number:11111)
4. after push the connect button, you should be able to see the file explorer, and select file that you want to downloading
you should be able to see the progress bar that showing downloading size. when downloading is finished, you should find downloading fild data(bytes) at Stm32wb55 Nucleo Pack board flash memory address 0x0800 7000
