@Component
export default class ExTableColumnEditor extends Vue {
    @Prop({ type: Array, default: () => [] }) value: [];

    dlgVisible = false;
    column = null;
    treeProps = {
        label: 'title',
        children: 'children'
    };

    get field() {
        return this.column ? this.column.field : '';
    }
    set field(val) {
        if (this.column) {
            this.$set(this.column, 'field', val);
        }
    }

    get title() {
        return this.column ? this.column.title : '';
    }
    set title(val) {
        if (this.column) {
            this.$set(this.column, 'title', val);
        }
    }

    get width() {
        return this.column ? this.column.width : 0;
    }
    set width(val) {
        if (this.column) {
            this.$set(this.column, 'width', val);
        }
    }

    onCurrentChanged(col, node) {
        this.column = col;
    }

    onAdd() {
        let col = { title: '标题', field: '', width: 100 };
        this.value.push(col);
    }

}
