.NOTINTERMEDIATE:

testcases=$(shell ls test/*/main.py | awk -F '/' '{print $$2}')
default: $(addprefix run-,$(testcases))
gen=gen/filelist.f
$(gen)&: $(wildcard src/main/scala/*)
	mill ChiselAIA
compile=test/sim_build/Vtop
$(compile): $(gen)
	make -C test/dir ../sim_build/Vtop
# let the `make run-...` can be auto completed
define RUN_TESTCASE =
run-$(1):
endef
$(foreach testcase,$(testcases),$(eval $(call RUN_TESTCASE,$(testcase))))
run-%: test/%/main.py $(compile)
	# `ulimit -s` make sure stack size is enough
	ulimit -s 211487 && make -C $(dir $<) -f ../dir/Makefile
clean:
	rm -rf out/ gen/ test/sim_build/

################################################################################
# doc
################################################################################
MDs=$(shell find docs/ -name "*.md") docs/Params.md
PYSVGs=$(subst _dot.py,_py.svg,$(shell find docs/ -name "*_dot.py"))
DRAWIOSVGs=$(subst .drawio,.svg,$(shell find docs/ -name "*.drawio"))
DOTSVGs=$(subst .dot,.svg,$(shell find docs/ -name "*.dot"))
doc: $(MDs) $(PYSVGs) $(DRAWIOSVGs) $(DOTSVGs)
	mdbook build
docs/images/arch_configure_py.dot: docs/images/arch_common.py
docs/images/arch_interrupt_py.dot: docs/images/arch_common.py
%_py.dot: %_dot.py
	python3 $<
%.svg: %.dot
	dot -Tsvg $< -o $@
	# css can only recognize intrinsic size in px
	# https://developer.mozilla.org/en-US/docs/Glossary/Intrinsic_Size
	sed -i 's/\([0-9]\+\)pt/\1px/g' $@
%.svg: %.drawio
	drawio -x -f svg $<
docs/Params.md: src/main/scala/Params.scala
	markcode $< > $@
