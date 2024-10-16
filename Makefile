testcases=$(shell ls test/*/main.py | awk -F '/' '{print $$2}')
default: $(addprefix run-,$(testcases))
gen=$(wildcard gen/*) gen/
$(gen)&: $(wildcard src/main/scala/*)
	mill OpenAIA
compile=test/sim_build/Vtop
$(compile): $(gen)
	make -C test/dir ../sim_build/Vtop
run-%: test/%/main.py $(compile)
	# `ulimit -s` make sure stack size is enough
	ulimit -s 211487 && make -C $(dir $<) -f ../dir/Makefile
