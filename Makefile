testcases=$(shell ls test/*/main.py | awk -F '/' '{print $$2}')
default: $(addprefix run-,$(testcases))
gen=gen/filelist.f
$(gen)&: $(wildcard src/main/scala/*)
	mill OpenAIA
compile=test/sim_build/Vtop
$(compile): $(gen)
	make -C test/dir ../sim_build/Vtop
run-%: test/%/main.py $(compile)
	# `ulimit -s` make sure stack size is enough
	ulimit -s 211487 && make -C $(dir $<) -f ../dir/Makefile
clean:
	rm -rf out/ gen/ test/sim_build/

################################################################################
# doc
################################################################################
doc: ./docs/images/arch.py.dot
%.py.dot: %.dot.py
	python3 $<
