package issue75

import grails.transaction.Transactional

@Transactional
class MyService {

    def serviceMethod(boolean skipMainBranch = false) {
        if (skipMainBranch) {
            return 'skipped main service branch'
        } else {
            return 'hit main service branch'
        }
    }
}
