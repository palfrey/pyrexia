configFile = "config.json"
if file.open(configFile) then
  config = cjson.decode(file.read())
else
  config = {}
end
for key,value in pairs(config) do print(key .. " = " .. value) end

wifi.setmode(wifi.STATIONAP)

cfg={}
cfg.ssid= "temp" .. "-" .. wifi.ap.getmac()
cfg.pwd= "password"
cfg.beacon = 100
cfg.auth = AUTH_OPEN
wifi.ap.config(cfg)

if config["ssid"] ~= nil and config["password"] ~= nil then
  -- wifi.sta.eventMonReg(wifi.STA_IDLE, function() print("STATION_IDLE") end)
  -- wifi.sta.eventMonReg(wifi.STA_CONNECTING, function() print("STATION_CONNECTING") end)
  -- wifi.sta.eventMonReg(wifi.STA_WRONGPWD, function() print("STATION_WRONG_PASSWORD") end)
  -- wifi.sta.eventMonReg(wifi.STA_APNOTFOUND, function() print("STATION_NO_AP_FOUND") end)
  -- wifi.sta.eventMonReg(wifi.STA_FAIL, function() print("STATION_CONNECT_FAIL") end)
  -- wifi.sta.eventMonReg(wifi.STA_GOTIP, function()
  --   print("STATION_GOT_IP")
  --   print(wifi.sta.getip())
  -- end)
  -- wifi.sta.eventMonStart()
  wifi.sta.config(config["ssid"], config["password"])
  -- while wifi.sta.status() ~= 5 do
  --   print(wifi.sta.status())
  --   tmr.delay(100000)
  -- end
  print(wifi.sta.getip())
end

function default(value, d)
  if value == nil then
    return d
  else
    return value
  end
end

sv=net.createServer(net.TCP,30)
sv:listen(80,function(c)
  c:on("receive", function(c, pl)
    if pl:find("POST") ~= nil then
      -- print(pl)
      local start, finish = pl:find("\r?\n\r?\n") -- two newlines
      print(finish)
      local form = pl:sub(finish + 1)
      print("form: \'" .. form .. "\'")
      for arg in form:gmatch("[^&=]+=[^&]*") do
        local key, value = arg:match("([^=]+)=(.*)")
        print("key: " .. key)
        print("value: " .. value)
        config[key] = value
      end
      file.open(configFile, "w+")
      file.write(cjson.encode(config))
      file.close()
    end
    c:send("HTTP/1.1 200 OK\r\n")
    c:send("Connection: close\r\n\r\n")
    c:send("<html lang='en'> ")
    c:send("<body> ")
    c:send("<h1>Temperature sensor setup</h1> ")
    c:send("<form method=\"POST\">")
    c:send("Wifi SSID: <input type=\"text\" name=\"ssid\" value=\"" .. default(config["ssid"], "") .. "\" /><br />")
    c:send("Wifi Password: <input type=\"text\" name=\"password\" value=\"" .. default(config["password"], "") .. "\" /><br />")
    c:send("<input type=\"submit\" value=\"Save config\" />")
    c:send("</form>")
    c:send("</body>")
    c:close()
  end)
end)
