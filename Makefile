compile-all:
	javac game_server_proj/*.java -d "./game_server_proj/bin"

run-server:
	java -classpath "./game_server_proj/bin" game_server_proj/Server 8082

run-client-1:
	mkdir -p Client-1
	cd Client-1; java -classpath "../game_server_proj/bin" game_server_proj/Client 127.0.0.1 8082

run-client-2:
	mkdir -p Client-2
	cd Client-2; java -classpath "../game_server_proj/bin" game_server_proj/Client 127.0.0.1 8082

run-client-3:
	mkdir -p Client-3
	cd Client-3; java -classpath "../game_server_proj/bin" game_server_proj/Client 127.0.0.1 8082

run-client-4:
	mkdir -p Client-4
	cd Client-4; java -classpath "../game_server_proj/bin" game_server_proj/Client 127.0.0.1 8082

run-client-5:
	mkdir -p Client-5
	cd Client-5; java -classpath "../game_server_proj/bin" game_server_proj/Client 127.0.0.1 8082