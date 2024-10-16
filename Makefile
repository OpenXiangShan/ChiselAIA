default: run
gen: $(wildcard src/main/scala/*)
	mill OpenAIA
compile=test/sim_build/Vtop
$(compile): $(wildcard gen/*)
	make -C test/ sim_build/Vtop
run: $(compile)
	# `ulimit -s` make sure stack size is enough
	ulimit -s 211487 && make -C test/
