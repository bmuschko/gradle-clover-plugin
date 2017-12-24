package issue75

class MyController {

    def index() {
        if (params.skipMainBranch) {
            return 'skipped main controller branch'
        } else {
            return 'hit main controller branch'
        }
    }
}
