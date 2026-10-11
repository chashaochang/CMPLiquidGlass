import { ArkUIViewController, Compose } from '@cpf-kmp-cmp/compose';
import nativeApi from 'libentry.so';

@Entry
@Component
struct Index {
  private controller: ArkUIViewController | undefined = undefined

  aboutToAppear(): void {
    this.controller = nativeApi.MainArkUIViewController() as ArkUIViewController
  }

  onPageShow(): void {
    this.controller?.onPageShow()
  }

  onPageHide(): void {
    this.controller?.onPageHide()
  }

  onBackPress(): boolean | void {
    return this.controller ? this.controller.onBackPress() : false
  }

  build() {
    Stack() {
      if (this.controller) {
        Compose({
          controller: this.controller,
          libraryName: 'entry',
          onBackPressed: () => this.controller ? this.controller.onBackPress() : false
        })
          .width('100%')
          .height('100%')
      }
    }
    .width('100%')
    .height('100%')
  }
}
