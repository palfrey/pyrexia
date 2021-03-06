local configFile = "config.json"
if file.open(configFile) then
	config = cjson.decode(file.read())
else
	config = {}
end
for key,value in pairs(config) do print(key .. " = " .. value) end

-- wifi.setmode(wifi.STATIONAP)
wifi.setmode(wifi.STATION)

local name = "temp" .. "-" .. wifi.sta.getmac()

-- local cfg={}
-- cfg.ssid = name
-- cfg.pwd = "password"
-- cfg.beacon = 100
-- cfg.auth = AUTH_OPEN
-- wifi.ap.config(cfg)

function readTemp(pin)
	status,temp,humi = dht.read(pin)
	if( status == dht.OK ) then
		print("DHT Temperature:"..temp..";".."Humidity:"..humi)
	elseif( status == dht.ERROR_CHECKSUM ) then
		print( "DHT Checksum error." );
	elseif( status == dht.ERROR_TIMEOUT ) then
		print( "DHT Time out." );
	else
		print("Something else")
		print(status)
	end
end

if config["ssid"] ~= nil and config["password"] ~= nil then
	wifi.sta.eventMonReg(wifi.STA_IDLE, function() print("STATION_IDLE") end)
	wifi.sta.eventMonReg(wifi.STA_CONNECTING, function() print("STATION_CONNECTING") end)
	wifi.sta.eventMonReg(wifi.STA_WRONGPWD, function() print("STATION_WRONG_PASSWORD") end)
	wifi.sta.eventMonReg(wifi.STA_APNOTFOUND, function() print("STATION_NO_AP_FOUND") end)
	wifi.sta.eventMonReg(wifi.STA_FAIL, function() print("STATION_CONNECT_FAIL") end)
	wifi.sta.eventMonReg(wifi.STA_GOTIP,
		function()
			print("STATION_GOT_IP")
			print(wifi.sta.getip())

			if config["mqttHost"] ~= nil and config["mqttUser"] ~= nil then
				print ("connecting to " .. config["mqttHost"])
				local m = mqtt.Client(name, 120, config["mqttUser"], config["mqttPassword"])
				m:on("offline", function(con)
						print("Disconnected from MQTT")
						node.restart()
					end)
				m:connect(config["mqttHost"], config["mqttPort"], 0, function(conn)
						print("Connected to MQTT:" .. config["mqttHost"] .. ":" .. config["mqttPort"] .." as " .. name )
						local timer_count = 0
						tmr.alarm(0, 5000, 1, function()
								print("reading temperature " .. timer_count)
								timer_count = timer_count + 1
								local status, temp, humid = dht.read(4)
								if (status == dht.OK) then
									print("Got temperature " .. temp .. " and humidity " .. humid)
									local msg = {temp = temp, humid = humid, id = name }
									conn:publish("/temp", cjson.encode(msg), 0, 0, function(conn) print("sent") end)
								else
									print("Error status of temp sensor: " .. status)
									local msg = {status = status, id = name }
									conn:publish("/temp", cjson.encode(msg), 0, 0, function(conn) print("sent") end)
								end
								if timer_count == 12 * 5 then -- 5 minutes (12 x 5 second periods = 1 minute)
									print("restarting")
									node.restart()
								end
							end)
					end)
			else
				print("Missing either mqttHost or mqttUser from config")
			end
		end)
	print("status")
	print(wifi.sta.status())
	wifi.sta.eventMonStart()
	wifi.sta.config(config["ssid"], config["password"])
end

local function default(value, d)
	if value == nil then
		return d
	else
		return value
	end
end

-- sv=net.createServer(net.TCP,30)
-- sv:listen(80,function(c)
-- c:on("receive", function(c, pl)
-- if pl:find("POST") ~= nil then
-- -- print(pl)
-- local start, finish = pl:find("\r?\n\r?\n") -- two newlines
-- print(finish)
-- local form = pl:sub(finish + 1)
-- print("form: \'" .. form .. "\'")
-- for arg in form:gmatch("[^&=]+=[^&]*") do
-- local key, value = arg:match("([^=]+)=(.*)")
-- print("key: " .. key)
-- print("value: " .. value)
-- config[key] = value
-- end
-- file.open(configFile, "w+")
-- file.write(cjson.encode(config))
-- file.close()
-- end
-- c:send("HTTP/1.1 200 OK\r\n")
-- c:send("Connection: close\r\n\r\n")
-- c:send("<html lang='en'> ")
-- c:send("<body> ")
-- c:send("<h1>Temperature sensor setup</h1> ")
-- c:send("<form method=\"POST\">")
-- c:send("Wifi SSID: <input type=\"text\" name=\"ssid\" value=\"" .. default(config["ssid"], "") .. "\" /><br />")
-- c:send("Wifi Password: <input type=\"text\" name=\"password\" value=\"" .. default(config["password"], "") .. "\" /><br />")
-- c:send("MQTT Host: <input type=\"text\" name=\"mqttHost\" value=\"" .. default(config["mqttHost"], "") .. "\" /><br />")
-- c:send("MQTT Port: <input type=\"text\" name=\"mqttPort\" value=\"" .. default(config["mqttPort"], "1883") .. "\" /><br />")
-- c:send("MQTT User: <input type=\"text\" name=\"mqttUser\" value=\"" .. default(config["mqttUser"], "") .. "\" /><br />")
-- c:send("MQTT Password: <input type=\"text\" name=\"mqttPassword\" value=\"" .. default(config["mqttPassword"], "") .. "\" /><br />")
-- c:send("<input type=\"submit\" value=\"Save config\" />")
-- c:send("</form>")
-- c:send("</body>")
-- c:close()
-- end)
-- end)
