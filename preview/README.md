本目录内存放用于预览的临时编译的公共flutter包,需要打包至运行时

# build dartdevc
```shell
dart compile exe -o /media/psf/Home/Projects/AppBoxFuture/appbox.java/preview/dartdevc bin/dartdevc2.dart
```

# build flutter_web.js
```shell
/home/rick/snap/flutter/common/flutter/bin/cache/dart-sdk/bin/dartdevc -s /home/rick/snap/flutter/common/flutter/bin/cache/flutter_web_sdk/flutter_web_sdk/kernel/flutter_ddc_sdk_sound.dill --packages=bootstrap/.dart_tool/package_config.json --modules=amd --sound-null-safety --enable-experiment=non-nullable -o flutter_web.js  package:flutter/animation.dart package:flutter/cupertino.dart package:flutter/foundation.dart package:flutter/gestures.dart package:flutter/material.dart package:flutter/painting.dart package:flutter/physics.dart package:flutter/rendering.dart package:flutter/scheduler.dart package:flutter/semantics.dart package:flutter/services.dart package:flutter/widgets.dart
```

# build get
```shell
/home/rick/snap/flutter/common/flutter/bin/cache/dart-sdk/bin/dartdevc -s /media/psf/Home/Projects/AppBoxFuture/appbox.java/preview/flutter_web.dill -s /home/rick/snap/flutter/common/flutter/bin/cache/flutter_web_sdk/flutter_web_sdk/kernel/flutter_ddc_sdk_sound.dill --packages=bootstrap/.dart_tool/package_config.json --modules=amd --module-name=get --sound-null-safety --enable-experiment=non-nullable -o /media/psf/Home/Projects/AppBoxFuture/appbox.java/preview/get.js package:get/get.dart
```

# build bootstrap main
```shell
/home/rick/snap/flutter/common/flutter/bin/cache/dart-sdk/bin/dartdevc -s /media/psf/Home/Projects/AppBoxFuture/appbox.java/preview/flutter_web.dill -s /home/rick/snap/flutter/common/flutter/bin/cache/flutter_web_sdk/flutter_web_sdk/kernel/flutter_ddc_sdk_sound.dill --packages=bootstrap/.dart_tool/package_config.json --modules=amd --no-summarize --module-name=get --sound-null-safety --enable-experiment=non-nullable -o /media/psf/Home/Projects/AppBoxFuture/appbox.java/preview/main.dart.js package:appbox/main.dart
```

# build view test
```shell
/media/psf/Home/Projects/AppBoxFuture/appbox.java/preview/dartdevc --dart-sdk-summary=/home/rick/snap/flutter/common/flutter/bin/cache/flutter_web_sdk/flutter_web_sdk/kernel/flutter_ddc_sdk_sound.dill -s /media/psf/Home/Projects/AppBoxFuture/appbox.java/preview/flutter_web.dill --modules=amd --inline-source-map --no-summarize --sound-null-safety --enable-experiment=non-nullable --packages=.dart_tool/package_config.json -o test.js package:appbox/sys/views/HomePage.dart
```
