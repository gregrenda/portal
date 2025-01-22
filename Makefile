# MIT License
# 
# Copyright (c) 2025 Greg Renda
# 
# Permission is hereby granted, free of charge, to any person obtaining a
# copy of this software and associated documentation files (the "Software"),
# to deal in the Software without restriction, including without limitation
# the rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.

Q := @
OUT := out
APP_NAME := portal
APK := $(OUT)/$(APP_NAME).apk
APP_PATH := org/renda/$(APP_NAME)
KEYSTORE := mykey.keystore
PASS := password
SRC := app
GEN := $(OUT)/gen
TREE := $(OUT)/tree
RES_DIR := $(SRC)/res
RESOURCES := $(GEN)/$(APP_PATH)/R.java

ANDROID_BUILD_IMAGE := docker.io/mingc/android-build-box:latest
SDK := /opt/android-sdk
TOOLS := $(SDK)/build-tools/35.0.0
AAPT := $Q $(TOOLS)/aapt
PLATFORM := $(SDK)/platforms/android-35
JAR := $(PLATFORM)/android.jar

define POD
    $Q podman run --rm -it						      \
	          -v .:/project						      \
	          -e JENV_LOADED=1					      \
                  -it -w /project/$(1) $(ANDROID_BUILD_IMAGE)
endef # POD

SRCS := $(wildcard $(SRC)/java/*.java)
SRC_FILE_NAMES := $(notdir $(basename $(SRCS)))
CLASS_FILES := $(addprefix $(GEN)/$(APP_PATH)/,				      \
			   $(addsuffix .class, $(SRC_FILE_NAMES)))
MANIFEST := $(SRC)/AndroidManifest.xml

all:
	$Q mkdir -p $(GEN) $(TREE)
	$(POD) make $(APK)

# sign the app
$(APK): $(APK).aligned $(KEYSTORE)
	$Q $(TOOLS)/apksigner sign --ks $(KEYSTORE) --ks-pass pass:$(PASS)    \
			           --out $@ $<

# generate resource file
$(RESOURCES): $(MANIFEST) $(JAR) $(RES_DIR)/layout/activity_main.xml
	$(AAPT) package -f -m -J $(GEN) -S $(RES_DIR) -M $(MANIFEST) -I $(JAR)

# compile java code to class files
$(CLASS_FILES) &: $(SRCS) $(RESOURCES)
	$Q javac -Xlint:deprecation -d $(GEN) -classpath $(JAR) $^

# Dalvik translation
$(TREE)/classes.dex: $(CLASS_FILES)
	$Q $(TOOLS)/d8 --release --lib $(JAR) --output $(TREE)		      \
	   $(GEN)/$(APP_PATH)/*.class

# package the app
$(APK).unsigned: $(TREE)/classes.dex
	$(AAPT) package -f -M $(MANIFEST) -S $(RES_DIR) -I $(JAR) -F $@ $(TREE)

# align the app
$(APK).aligned: $(APK).unsigned
	$Q $(TOOLS)/zipalign -f -p 4 $< $@

$(KEYSTORE):
	$Q keytool -genkeypair -validity 365 -keystore $@ -keyalg RSA	      \
		   -keysize 2048 -storepass $(PASS)			      \
                   -dname cn=myname,ou=mygroup,o=mycompany,c=mycountry

clean:
	$Q $(RM) -rf $(OUT)

allclean: clean
	$Q $(RM) -rf $(KEYSTORE)

install:
	$Q adb install -r $(APK)
