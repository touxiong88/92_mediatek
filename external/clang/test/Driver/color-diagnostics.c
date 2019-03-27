// RUN: %clang -fcolor-diagnostics -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=CD %s
// CHECK-CD: clang{{.*}}" "-fcolor-diagnostics"

// RUN: %clang -fno-color-diagnostics -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=NCD %s
// CHECK-NCD-NOT: clang{{.*}}" "-fcolor-diagnostics"

// RUN: %clang -fdiagnostics-color -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=DC %s
// CHECK-DC: clang{{.*}}" "-fcolor-diagnostics"

// RUN: %clang -fno-diagnostics-color -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=NDC %s
// CHECK-NDC-NOT: clang{{.*}}" "-fcolor-diagnostics"

// RUN: %clang -fdiagnostics-color=always -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=DCE_A %s
// CHECK-DCE_A: clang{{.*}}" "-fcolor-diagnostics"

// RUN: %clang -fdiagnostics-color=never -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=DCE_N %s
// CHECK-DCE_N-NOT: clang{{.*}}" "-fcolor-diagnostics"

// The test doesn't run in a PTY, so "auto" defaults to off.
// RUN: %clang -fdiagnostics-color=auto -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=DCE_AUTO %s
// CHECK-DCE_AUTO-NOT: clang{{.*}}" "-fcolor-diagnostics"

// RUN: %clang -fdiagnostics-color=foo -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=DCE_FOO %s
// CHECK-DCE_FOO: error: the clang compiler does not support '-fdiagnostics-color=foo'

// Check that the last flag wins.
// RUN: %clang -fno-color-diagnostics -fdiagnostics-color -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=NCD_DC_S %s
// CHECK-NCD_DC_S: clang{{.*}}" "-fcolor-diagnostics"

// RUN: %clang -fcolor-diagnostics -fno-diagnostics-color -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=CD_NDC_S %s
// CHECK-CD_NDC_S-NOT: clang{{.*}}" "-fcolor-diagnostics"

// RUN: %clang -fdiagnostics-color -fno-color-diagnostics -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=DC_NCD_S %s
// CHECK-DC_NCD_S-NOT: clang{{.*}}" "-fcolor-diagnostics"

// RUN: %clang -fno-diagnostics-color -fcolor-diagnostics -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=NDC_CD_S %s
// CHECK-NDC_CD_S: clang{{.*}}" "-fcolor-diagnostics"

// RUN: %clang -fcolor-diagnostics -fdiagnostics-color=auto -### -c %s 2>&1 \
// RUN:     | FileCheck --check-prefix=CD_DCE_AUTO_S %s
// CHECK-CD_DCE_AUTO_S-NOT: clang{{.*}}" "-fcolor-diagnostics"
